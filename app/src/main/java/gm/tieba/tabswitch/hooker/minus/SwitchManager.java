package gm.tieba.tabswitch.hooker.minus;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.dao.Preferences;

public class SwitchManager extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.adp.lib.featureSwitch.SwitchManager", sClassLoader,
                "findType", String.class, new XC_MethodHook() {
                    @Override
                    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (Preferences.getStringSet("switch_manager").contains((String) param.args[0])) {
                            param.setResult(-1);
                        }
                    }
                });
        if (Preferences.getStringSet("switch_manager").contains("flutter_person_center_enable_android_12")) {
            XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.view.AgreeView", sClassLoader,
                    "setAgreeAlone", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            StackTraceElement[] stes = new Exception().getStackTrace();
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
