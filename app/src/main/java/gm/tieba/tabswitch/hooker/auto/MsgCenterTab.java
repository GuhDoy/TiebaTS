package gm.tieba.tabswitch.hooker.auto;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.ReflectUtils;

public class MsgCenterTab extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "msg_center_tab";
    }

    @Override
    public void hook() throws Throwable {
        for (final var method : XposedHelpers.findClass("com.baidu.tieba.immessagecenter.msgtab.ui.view.MsgCenterContainerView", sClassLoader).getDeclaredMethods()) {
            if (method.getParameterTypes().length == 0 && method.getReturnType() == long.class) {
                XposedHelpers.findAndHookMethod("com.baidu.tieba.immessagecenter.msgtab.ui.view.MsgCenterContainerView", sClassLoader, method.getName(), new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        ReflectUtils.setObjectField(param.thisObject, Long.class, -1L);
                    }
                });
            }
        }
    }
}
