package gm.tieba.tabswitch.hooker.auto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedWrapper;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.util.ReflectUtils;
import gm.tieba.tabswitch.widget.Switch;

public class EyeshieldMode extends XposedWrapper implements IHooker {
    private static boolean sSavedUiMode;

    public void hook() throws Throwable {
        sSavedUiMode = DisplayUtils.isLightMode(getContext());
        SharedPreferences.Editor editor = getContext().getSharedPreferences("common_settings",
                Context.MODE_PRIVATE).edit();
        editor.putString("skin_", sSavedUiMode ? "0" : "1");
        editor.apply();
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader,
                "onConfigurationChanged", Configuration.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        if (sSavedUiMode != DisplayUtils.isLightMode(activity)) {
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
                        if (sSavedUiMode != DisplayUtils.isLightMode(activity)) {
                            sSavedUiMode = DisplayUtils.isLightMode(activity);
                            Switch bdSwitch = new Switch(activity.findViewById(
                                    ReflectUtils.getId("item_switch")));
                            if (DisplayUtils.isLightMode(activity)) bdSwitch.turnOff();
                            else bdSwitch.turnOn();
                            activity.finish();
                        }
                    }
                });
    }
}
