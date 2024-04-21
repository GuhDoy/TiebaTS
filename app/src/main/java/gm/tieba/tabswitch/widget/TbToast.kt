package gm.tieba.tabswitch.widget

import androidx.annotation.MainThread
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher
import gm.tieba.tabswitch.util.callStaticMethod
import gm.tieba.tabswitch.util.findFirstMethodByExactType

class TbToast : XposedContext(), Obfuscated {
    override fun matchers(): List<Matcher> {
        // setToastString()
        return listOf(StringMatcher("can not be call not thread! trace = "))
    }

    companion object {
        @JvmField
        var LENGTH_SHORT = 2000
        @JvmField
        var LENGTH_LONG = 3500

        @JvmStatic
        @MainThread
        fun showTbToast(text: String?, duration: Int) {
            AcRules.findRule("can not be call not thread! trace = ") { _, clazz, _ ->
                val md = findFirstMethodByExactType(
                    clazz,
                    String::class.java,
                    Int::class.javaPrimitiveType!!,
                    Boolean::class.javaPrimitiveType!!
                )
                runOnUiThread { callStaticMethod(md, text, duration, true) }
            }
        }
    }
}
