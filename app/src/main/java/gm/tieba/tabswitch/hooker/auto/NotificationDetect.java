package gm.tieba.tabswitch.hooker.auto;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;

public class NotificationDetect extends XposedContext implements IHooker {
    @NonNull
    @Override
    public String key() {
        return "notification_detect";
    }

    public void hook() throws Throwable {
        // 禁止检测通知开启状态
        XposedHelpers.findAndHookMethod(
                "androidx.core.app.NotificationManagerCompat",
                sClassLoader,
                "areNotificationsEnabled",
                XC_MethodReplacement.returnConstant(true)
        );
    }
}
