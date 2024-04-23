package gm.tieba.tabswitch.hooker.eliminate

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.Preferences.getLikeForum
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.widget.TbToast
import gm.tieba.tabswitch.widget.TbToast.Companion.showTbToast

class FollowFilter : XposedContext(), IHooker {
    override fun key(): String {
        return "follow_filter"
    }

    @Throws(Throwable::class)
    override fun hook() {
        hookBeforeMethod(
            "tbclient.Personalized.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val forums = getLikeForum() ?: run {
                runOnUiThread { showTbToast("暂未获取到关注列表", TbToast.LENGTH_LONG) }
                return@hookBeforeMethod
            }
            val threadList = XposedHelpers.getObjectField(param.thisObject, "thread_list") as? MutableList<*>
            threadList?.removeIf { o: Any? -> !forums.contains(XposedHelpers.getObjectField(o, "fname") as? String) }
        }
    }
}
