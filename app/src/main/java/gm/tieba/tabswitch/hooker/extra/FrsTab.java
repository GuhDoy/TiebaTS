package gm.tieba.tabswitch.hooker.extra;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.ReflectUtils;

public class FrsTab extends BaseHooker implements IHooker {
    private int mPosition;

    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.NavTabInfo$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "tab");
                if (list == null) return;
                for (int i = 0; i < list.size(); i++) {
                    if ((Integer) XposedHelpers.getObjectField(list.get(i), "tab_type") == 14) {
                        mPosition = i;
                        return;
                    }
                }
            }
        });
        AcRules.findRule(sRes.getString(R.string.FrsTab), new AcRules.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                if (!"com.baidu.tieba.frs.vc.FrsTabViewController".equals(clazz)) return;
                XposedHelpers.findAndHookMethod("com.baidu.tieba.frs.vc.FrsTabViewController", sClassLoader, method, new XC_MethodHook() {
                    @Override
                    public void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object viewPager = ReflectUtils.getObjectField(param.thisObject, "com.baidu.tieba.frs.FrsTabViewPager");
                        XposedHelpers.callMethod(viewPager, "setCurrentItem", mPosition, false);
                    }
                });
            }
        });
    }
}
