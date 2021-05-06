package gm.tieba.tabswitch.hooker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.Hooker;
import gm.tieba.tabswitch.util.DisplayHelper;

public class EyeshieldMode extends BaseHooker implements Hooker {
    private static boolean sSavedUiMode;

    public void hook() throws Throwable {
        sSavedUiMode = DisplayHelper.isLightMode(sContextRef.get());
        SharedPreferences.Editor editor = sContextRef.get().getSharedPreferences(
                "common_settings", Context.MODE_PRIVATE).edit();
        editor.putString("skin_", sSavedUiMode ? "0" : "1");
        editor.apply();
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onConfigurationChanged", Configuration.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (sSavedUiMode != DisplayHelper.isLightMode(activity)) {
                    Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.setting.more.MoreActivity");
                    activity.startActivity(intent);
                }
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.setting.more.MoreActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (sSavedUiMode != DisplayHelper.isLightMode(activity)) {
                    sSavedUiMode = DisplayHelper.isLightMode(activity);
                    View itemSwitch = activity.findViewById(sClassLoader.loadClass("com.baidu.tieba.R$id").getField("item_switch").getInt(null));
                    Class<?> BdSwitchView = sClassLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView");
                    if (DisplayHelper.isLightMode(activity)) {
                        try {
                            BdSwitchView.getDeclaredMethod("turnOff").invoke(itemSwitch);
                        } catch (NoSuchMethodException e) {
                            BdSwitchView.getDeclaredMethod("f").invoke(itemSwitch);
                        }
                    } else {
                        try {
                            BdSwitchView.getDeclaredMethod("turnOn").invoke(itemSwitch);
                        } catch (NoSuchMethodException e) {
                            BdSwitchView.getDeclaredMethod("i").invoke(itemSwitch);
                        }
                    }
                    activity.finish();
                }
            }
        });
    }
}
