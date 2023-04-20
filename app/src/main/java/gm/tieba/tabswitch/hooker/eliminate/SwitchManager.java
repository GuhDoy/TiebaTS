package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.IHooker;

public class SwitchManager extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "switch_manager";
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.adp.lib.featureSwitch.SwitchManager", sClassLoader,
                "findType", String.class, new XC_MethodHook() {
                    @Override
                    public void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                        if (Preferences.getStringSet("switch_manager").contains((String) param.args[0])) {
                            param.setResult(-1);
                        }
                    }
                });
        if (Preferences.getStringSet("switch_manager").contains("flutter_person_center_enable_android_12")) {
            XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.view.AgreeView", sClassLoader,
                    "setAgreeAlone", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            final StackTraceElement[] stes = new Exception().getStackTrace();
                            for (int i = 5; i < 20; i++) {
                                if (stes[i].getClassName().equals(
                                        "com.baidu.tbadk.core.view.ThreadCommentAndPraiseInfoLayout")) {
                                    param.args[0] = true;
                                    return;
                                }
                            }
                        }
                    });
        }
    }
}
