package gm.tieba.tabswitch.hooker;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.BuildConfig;
import gm.tieba.tabswitch.IHooker;

public class Hide extends BaseHooker implements IHooker {
    @Override
    public void hook() throws Throwable {
        Class<?>[] classes = new Class<?>[]{Throwable.class, Thread.class};
        for (Class<?> clazz : classes) {
            XposedHelpers.findAndHookMethod(clazz, "getStackTrace", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    StackTraceElement[] stes = (StackTraceElement[]) param.getResult();
                    List<StackTraceElement> filtered = new ArrayList<>();
                    for (StackTraceElement ste : stes) {
                        String name = ste.getClassName();
                        if (!name.contains("posed") && !name.contains("Hooker")
                                && !name.contains(BuildConfig.APPLICATION_ID)) {
                            filtered.add(ste);
                        }
                    }

                    StackTraceElement[] result = new StackTraceElement[filtered.size()];
                    for (int i = 0; i < filtered.size(); i++) {
                        result[i] = filtered.get(i);
                    }
                    param.setResult(result);
                    super.afterHookedMethod(param);
                }
            });
        }
    }
}
