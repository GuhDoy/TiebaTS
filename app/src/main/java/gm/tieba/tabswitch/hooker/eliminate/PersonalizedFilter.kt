package gm.tieba.tabswitch.hooker.eliminate

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.util.parsePbContent

class PersonalizedFilter : XposedContext(), IHooker, RegexFilter {
    override fun key(): String {
        return "personalized_filter"
    }

    @Throws(Throwable::class)
    override fun hook() {
        hookBeforeMethod(
            "tbclient.Personalized.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val threadList = XposedHelpers.getObjectField(param.thisObject, "thread_list") as? MutableList<*>
            val pattern = getPattern() ?: return@hookBeforeMethod
            threadList?.removeIf { threadItem ->
                pattern.matcher(parsePbContent(threadItem, "first_post_content")).find() ||
                        pattern.matcher(XposedHelpers.getObjectField(threadItem, "title") as? String ?: "").find()
            }
        }
    }
}
