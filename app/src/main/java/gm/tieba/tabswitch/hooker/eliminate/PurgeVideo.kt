package gm.tieba.tabswitch.hooker.eliminate

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker

class PurgeVideo : XposedContext(), IHooker {
    override fun key(): String {
        return "purge_video"
    }

    @Throws(Throwable::class)
    override fun hook() {
        hookBeforeMethod(
            "tbclient.Personalized.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val threadList = XposedHelpers.getObjectField(param.thisObject, "thread_list") as? MutableList<*>
            threadList?.removeIf { o: Any? -> XposedHelpers.getObjectField(o, "video_info") != null }
        }
    }
}
