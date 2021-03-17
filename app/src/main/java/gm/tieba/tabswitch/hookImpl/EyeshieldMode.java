package gm.tieba.tabswitch.hookImpl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.util.DisplayHelper;

public class EyeshieldMode extends Hook {
    private static boolean savedUiMode;

    public static void hook(ClassLoader classLoader, Context context) throws Throwable {
        SharedPreferences sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("key_is_follow_system_mode", false)) return;
        savedUiMode = DisplayHelper.isLightMode(context);
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onConfigurationChanged", Configuration.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (savedUiMode != DisplayHelper.isLightMode(activity)) {
                    Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.setting.more.MoreActivity");
                    activity.startActivity(intent);
                }
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.setting.more.MoreActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (savedUiMode != DisplayHelper.isLightMode(activity)) {
                    savedUiMode = DisplayHelper.isLightMode(activity);
                    View itemSwitch = activity.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("item_switch").getInt(null));
                    Class<?> BdSwitchView = classLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView");
                    if (DisplayHelper.isLightMode(activity))
                        try {
                            BdSwitchView.getDeclaredMethod("turnOff").invoke(itemSwitch);
                        } catch (NoSuchMethodException e) {
                            BdSwitchView.getDeclaredMethod("f").invoke(itemSwitch);
                        }
                    else
                        try {
                            BdSwitchView.getDeclaredMethod("turnOn").invoke(itemSwitch);
                        } catch (NoSuchMethodException e) {
                            BdSwitchView.getDeclaredMethod("i").invoke(itemSwitch);
                        }
                    activity.finish();
                }
            }
        });
    }
}