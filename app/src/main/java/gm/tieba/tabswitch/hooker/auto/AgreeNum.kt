package gm.tieba.tabswitch.hooker.auto

import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker

class AgreeNum : XposedContext(), IHooker {

    override fun key(): String {
        return "agree_num"
    }

    override fun hook() {
        hookBeforeMethod(
            "tbclient.Agree\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            XposedHelpers.setObjectField(
                param.thisObject, "agree_num",
                XposedHelpers.getObjectField(param.thisObject, "diff_agree_num")
            )
        }
    }
}
