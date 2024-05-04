package gm.tieba.tabswitch.hooker.auto

import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker

class NotificationDetect : XposedContext(), IHooker {

    override fun key(): String {
        return "notification_detect"
    }

    override fun hook() {
        // 禁止检测通知开启状态
        hookReplaceMethod(
            "androidx.core.app.NotificationManagerCompat",
            "areNotificationsEnabled"
        ) { true }
        hookReplaceMethod(
            "com.baidu.tieba.push.PushSceneGroup",
            "getLimit"
        ) { 0 }
    }
}
