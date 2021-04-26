package gm.tieba.tabswitch.hooker;

import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.Hooker;

public class FrsTab extends BaseHooker implements Hooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.NavTabInfo$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "tab");
                if (list == null) return;
                for (int i = 0; i < list.size(); i++) {
                    if ((int) XposedHelpers.getObjectField(list.get(i), "tab_type") == 13) {
                        Collections.swap(list, i, i + 1);
                        return;
                    }
                }
            }
        });
    }
}