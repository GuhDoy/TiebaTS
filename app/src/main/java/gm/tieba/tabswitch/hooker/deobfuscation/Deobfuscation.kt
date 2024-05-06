package gm.tieba.tabswitch.hooker.deobfuscation

import android.content.Context
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules.putRule
import gm.tieba.tabswitch.dao.Preferences.putSignature
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.result.MethodDataList
import java.util.Objects
import java.util.zip.ZipFile

class Deobfuscation : XposedContext() {

    private val matchers: MutableList<Matcher> = ArrayList()
    private lateinit var packageResource: String

    fun setMatchers(matchers: List<Matcher>) {
        this.matchers.clear()
        this.matchers.addAll(matchers)
    }

    private fun <T> forEachProgressed(
        hooker: DeobfuscationHooker,
        collection: Collection<T>,
        action: (T) -> Unit
    ) {
        val size = collection.size
        collection.forEachIndexed { index, item ->
            hooker.progress = (index + 1).toFloat() / size
            action(item)
        }
    }

    fun dexkit(context: Context, hooker: DeobfuscationHooker) {
        load("dexkit")
        packageResource = context.packageResourcePath
        val bridge = DexKitBridge.create(packageResource)
        Objects.requireNonNull(bridge)

        forEachProgressed(hooker, matchers) { matcher: Matcher ->
            val methodDataList = MethodDataList()

            matcher.classMatcher?.let { classMatcher ->
                bridge.findClass(FindClass.create().matcher(classMatcher))
                    .flatMapTo(methodDataList) { classData ->
                        findMethod(bridge, FindMethod.create().searchPackages(classData.name), matcher)
                    }
            } ?: methodDataList.addAll(findMethod(bridge, FindMethod.create(), matcher))

            methodDataList.forEach { methodData ->
                putRule(matcher.toString(), methodData.className, methodData.name)
            }
        }

        bridge.close()
    }

    private fun findMethod(bridge: DexKitBridge, baseMethodQuery: FindMethod, matcher: Matcher): MethodDataList {
        return bridge.findMethod(baseMethodQuery.matcher(matcher.methodMatcher))
    }

    fun saveDexSignatureHashCode() {
        ZipFile(packageResource).use { apk ->
            apk.getInputStream(apk.getEntry("classes.dex")).use { inputStream ->
                val signatureHashCode = DeobfuscationHelper.calcSignature(inputStream).contentHashCode()
                putSignature(signatureHashCode)
            }
        }
    }
}
