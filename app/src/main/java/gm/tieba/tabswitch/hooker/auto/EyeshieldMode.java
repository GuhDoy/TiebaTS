package gm.tieba.tabswitch.hooker.auto;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.util.ReflectUtils;

public class EyeshieldMode extends XposedContext implements IHooker {
    private boolean mLastUiMode;

    @NonNull
    @Override
    public String key() {
        return "eyeshield_mode";
    }

    @Override
    public void hook() throws Throwable {
        mLastUiMode = DisplayUtils.isLightMode(getContext());
        SharedPreferences.Editor editor = getContext().getSharedPreferences("common_settings",
                Context.MODE_PRIVATE).edit();
        editor.putString("skin_", mLastUiMode ? "0" : "1");
        editor.apply();
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onConfigurationChanged", Configuration.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var activity = (Activity) param.thisObject;
                if (mLastUiMode != DisplayUtils.isLightMode(activity)) {
                    mLastUiMode = DisplayUtils.isLightMode(activity);

                    // com.baidu.tieba.setting.more.MoreActivity.OnSwitchStateChange()
                    if (!DisplayUtils.isLightMode(activity)) {
                        // CAM_X0201_1
                        var color = ReflectUtils.getColor("CAM_X0201");
                        XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("com.baidu.tbadk.core.util.UtilHelper", sClassLoader),
                                "setNavigationBarBackground", activity, color
                        );

                        XposedHelpers.callMethod(activity, "onChangeSkinType", 1);
                        var app = XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("com.baidu.tbadk.core.TbadkCoreApplication", sClassLoader),
                                "getInst"
                        );
                        XposedHelpers.callMethod(app, "setSkinType", 1);
                    } else {
                        XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("com.baidu.tbadk.core.util.SkinManager", sClassLoader),
                                "setDayOrDarkSkinTypeWithSystemMode", true, false
                        );
                    }
                }
            }
        });
    }
}
