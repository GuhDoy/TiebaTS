package gm.tieba.tabswitch.hooker.auto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.Preferences.getIsSigned
import gm.tieba.tabswitch.dao.Preferences.putSignDate
import gm.tieba.tabswitch.hooker.IHooker
import java.util.Calendar

class OpenSign : XposedContext(), IHooker {
    override fun key(): String {
        return "open_sign"
    }

    @Throws(Throwable::class)
    override fun hook() {
        hookAfterMethod(
            "com.baidu.tieba.tblauncher.MainTabActivity",
            "onCreate", Bundle::class.java
        ) { param ->
            val activity = param.thisObject as Activity
            if (!getIsSigned() && Calendar.getInstance()[Calendar.HOUR_OF_DAY] != 0) {
                val intent = Intent().setClassName(activity, "com.baidu.tieba.signall.SignAllForumActivity")
                activity.startActivity(intent)
            }
        }
        hookAfterMethod(
            "com.baidu.tieba.signall.SignAllForumActivity",
            "onClick", View::class.java
        ) { param ->
            val activity = param.thisObject as Activity
            if (!getIsSigned() && Calendar.getInstance()[Calendar.HOUR_OF_DAY] != 0) {
                putSignDate()
                activity.finish()
            }
        }
    }
}
