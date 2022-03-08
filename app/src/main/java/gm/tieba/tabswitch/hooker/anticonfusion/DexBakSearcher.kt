package gm.tieba.tabswitch.hooker.anticonfusion

import android.text.TextUtils
import org.jf.baksmali.Adaptors.ClassDefinition
import org.jf.baksmali.Adaptors.MethodDefinition
import org.jf.baksmali.BaksmaliOptions
import org.jf.baksmali.formatter.BaksmaliWriter
import org.jf.dexlib2.Format
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.instruction.DualReferenceInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.iface.reference.Reference.InvalidReferenceException
import org.jf.dexlib2.iface.reference.StringReference
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class DexBakSearcher {
    val stringMatchers = mutableListOf<String>()
    val literalMatchers = mutableListOf<Long>()
    val smaliMatchers = mutableListOf<String>()

    constructor(genericMatchers: Iterable<String>) {
        genericMatchers.forEach {
            when {
                it.startsWith('\"') && it.endsWith('\"') ->
                    stringMatchers.add(it.substring(1, it.length - 1))
                TextUtils.isDigitsOnly(it) ->
                    literalMatchers.add(it.toLong())
                else ->
                    smaliMatchers.add(it)
            }
        }
    }

    constructor(genericMatchers: Iterable<String>, literalMatchers: Iterable<Long>)
            : this(genericMatchers) {
        this.literalMatchers.addAll(literalMatchers)
    }

    constructor(
        stringMatchers: Iterable<String> = emptyList(),
        literalMatchers: Iterable<Long> = emptyList(),
        smaliMatchers: Iterable<String> = emptyList()
    ) {
        this.stringMatchers.addAll(stringMatchers)
        this.literalMatchers.addAll(literalMatchers)
        this.smaliMatchers.addAll(smaliMatchers)
    }

    // @see org.jf.baksmali.Adaptors.Format.InstructionMethodItem.writeTo()
    fun ClassDef.searchStringAndLiteral(l: MatcherListener) {
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
                    Opcode.CONST_4, Opcode.CONST_16, Opcode.CONST, Opcode.CONST_HIGH16,
                    Opcode.CONST_WIDE_16, Opcode.CONST_WIDE_32, Opcode.CONST_WIDE,
                    Opcode.CONST_WIDE_HIGH16 -> if (instruction.opcode.format in arrayOf(
                            Format.Format11n, Format.Format21ih, Format.Format21lh,
                            Format.Format21s, Format.Format31i, Format.Format51l,
                            Format.Format22b, Format.Format22s
                        ) && instruction is WideLiteralInstruction
                    ) {
                        val wideLiteral = instruction.wideLiteral
                        literalMatchers.forEach {
                            if (wideLiteral == it) {
                                l.onMatch(it, type.convert(), method.name)
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

    fun String.convert() = substring(indexOf("L") + 1, indexOf(";")).replace("/", ".")

    fun String.revert() = "L" + replace(".", "/") + ";"

    abstract class MatcherListener {
        open fun onMatch(matcher: String, clazz: String, method: String) {}
        open fun onMatch(matcher: Long, clazz: String, method: String) {}
    }
}
