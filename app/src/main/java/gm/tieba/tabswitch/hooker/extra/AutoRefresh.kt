package gm.tieba.tabswitch.hooker.extra

import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher

class AutoRefresh : XposedContext(), IHooker, Obfuscated {

    override fun key(): String {
        return "auto_refresh"
    }

    override fun matchers(): List<Matcher> {
        return listOf(
            StringMatcher("recommend_frs_refresh_time")
        )
    }

    override fun hook() {
        findRule(matchers()) { _, clazz, method ->
            val md = XposedHelpers.findMethodExactIfExists(
                findClass(clazz),
                method,
                Boolean::class.javaPrimitiveType
            ) ?: XposedHelpers.findMethodExactIfExists(
                findClass(clazz),
                method
            )
            md?.let { hookReplaceMethod(it) { false } }
        }
    }
}
