package gm.tieba.tabswitch.hooker.auto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.Reflect;
import gm.tieba.tabswitch.widget.Switch;

public class EyeshieldMode extends BaseHooker implements IHooker {
    private static boolean sSavedUiMode;

    public void hook() throws Throwable {
        sSavedUiMode = DisplayHelper.isLightMode(getContext());
        SharedPreferences.Editor editor = getContext().getSharedPreferences("common_settings",
                Context.MODE_PRIVATE).edit();
        editor.putString("skin_", sSavedUiMode ? "0" : "1");
        editor.apply();
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader,
                "onConfigurationChanged", Configuration.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        if (sSavedUiMode != DisplayHelper.isLightMode(activity)) {
                            Intent intent = new Intent().setClassName(activity,
                                    "com.baidu.tieba.setting.more.MoreActivity");
                            activity.startActivity(intent);
                        }
                    }
                });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.setting.more.MoreActivity", sClassLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        if (sSavedUiMode != DisplayHelper.isLightMode(activity)) {
                            sSavedUiMode = DisplayHelper.isLightMode(activity);
                            Switch bdSwitch = new Switch(activity.findViewById(
                                    Reflect.getId("item_switch")));
                            if (DisplayHelper.isLightMode(activity)) bdSwitch.turnOff();
                            else bdSwitch.turnOn();
                            activity.finish();
                        }
                    }
                });
    }
}
