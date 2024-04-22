package gm.tieba.tabswitch.hooker.auto;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;

public class AgreeNum extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "agree_num";
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.Agree$Builder", sClassLoader,
                "build", boolean.class, new XC_MethodHook() {
                    @Override
                    public void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField(param.thisObject, "agree_num",
                                XposedHelpers.getObjectField(param.thisObject, "diff_agree_num"));
                    }
                });
    }
}
