package gm.tieba.tabswitch.hooker.anticonfusion

import gm.tieba.tabswitch.util.MutablePair
import org.jf.baksmali.Adaptors.ClassDefinition
import org.jf.baksmali.Adaptors.MethodDefinition
import org.jf.baksmali.BaksmaliOptions
import org.jf.baksmali.formatter.BaksmaliWriter
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.instruction.DualReferenceInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.Reference.InvalidReferenceException
import org.jf.dexlib2.iface.reference.StringReference
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class DexBakSearcher constructor(matcherList: MutableList<String>) {
    private val stringMatchers: MutableList<String> by lazy { ArrayList() }
    private val smaliMatchers: MutableList<String> by lazy { ArrayList() }

    init {
        matcherList.forEach {
            when {
                it.startsWith('\"') && it.endsWith('\"') ->
                    stringMatchers.add(it.substring(1, it.length - 1))
                else ->
                    smaliMatchers.add(it)
            }
        }
    }

    fun ClassDef.searchString(l: MatcherListener) {
        for (method in methods) {
            val methodImpl = method.implementation ?: continue
            for (instruction in methodImpl.instructions) {
                when (instruction.opcode) {
                    Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO -> if (instruction is ReferenceInstruction) {
                        val reference = instruction.reference
                        try {
                            reference.validateReference()
                            if (reference is StringReference) {
                                val string = reference.string
                                stringMatchers.forEach {
                                    if (string == it) {
                                        l.onMatch("\"$it\"", type.convert(), method.name)
                                    }
                                }
                            }
                        } catch (ignored: InvalidReferenceException) {
                        }
                        if (instruction is DualReferenceInstruction) {
                            try {
                                val reference2 = instruction.reference2
                                reference2.validateReference()
                                if (reference2 is StringReference) {
                                    val string = reference2.string
                                    stringMatchers.forEach {
                                        if (string == it) {
                                            l.onMatch("\"$it\"", type.convert(), method.name)
                                        }
                                    }
                                }
                            } catch (ignored: InvalidReferenceException) {
                            }
                        }
                    }
                }
            }
        }
    }

    fun ClassDef.searchSmali(l: MatcherListener) {
        for (method in methods) {
            val methodImpl = method.implementation ?: continue
            val baos = ByteArrayOutputStream()
            val bufWriter = BufferedWriter(OutputStreamWriter(baos, StandardCharsets.UTF_8))
            BaksmaliWriter(bufWriter, null).use { writer ->
                val classDefinition = ClassDefinition(BaksmaliOptions(), this)
                val methodDefinition = MethodDefinition(classDefinition, method, methodImpl)
                methodDefinition.writeTo(writer)
                writer.flush()
                val smali = baos.toString()
                smaliMatchers.forEach {
                    if (smali.contains(it)) {
                        l.onMatch(it, type.convert(), method.name)
                    }
                }
            }
        }
    }

    fun String.convert(): String = substring(indexOf("L") + 1, indexOf(";")).replace("/", ".")

    fun String.revert(): String = "L" + replace(".", "/") + ";"

    fun <T> Collection<T>.most(): T {
        val map = ArrayList<MutablePair<T, Int>>()
        run loop@{
            forEach { thiz ->
                val pair = map.firstOrNull { it.first == thiz }
                if (pair == null) {
                    map.add(MutablePair(thiz, 0))
                } else {
                    pair.second++
                    if (pair.second > size / 2) return@loop
                }
            }
        }
        map.sortWith(Comparator.comparingInt { -it.second })
        return map[0].first
    }

    interface MatcherListener {
        fun onMatch(matcher: String, clazz: String, method: String)
    }
}
