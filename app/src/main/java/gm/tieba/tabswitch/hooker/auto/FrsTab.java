package gm.tieba.tabswitch.hooker.auto;

import androidx.annotation.NonNull;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;

public class FrsTab extends XposedContext implements IHooker, Obfuscated {

    @NonNull
    @Override
    public String key() {
        return "frs_tab";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(new StringMatcher("from_pb_or_person"));
    }

    private int mPosition;

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.NavTabInfo$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            public void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "tab");
                if (list == null) return;
                for (int i = 0; i < list.size(); i++) {
                    if ((Integer) XposedHelpers.getObjectField(list.get(i), "tab_type") == 14) {
                        mPosition = i;
                        return;
                    }
                }
            }
        });
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            if (!"com.baidu.tieba.frs.vc.FrsTabViewController".equals(clazz)) return;
            XposedHelpers.findAndHookMethod("com.baidu.tieba.frs.vc.FrsTabViewController", sClassLoader, method, new XC_MethodHook() {
                @Override
                public void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final Object viewPager = ReflectUtils.getObjectField(param.thisObject, "com.baidu.tieba.frs.FrsTabViewPager");
                    XposedHelpers.callMethod(viewPager, "setCurrentItem", mPosition, false);
                }
            });
        });
    }
}
