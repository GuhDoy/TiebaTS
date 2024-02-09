package gm.tieba.tabswitch.hooker.auto;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
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
        return List.of(
                new StringMatcher("forum_tab_current_list"),
                new StringMatcher("c/f/frs/page?cmd=301001&format=protobuf")
        );
    }

    private int mPosition;
    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            public void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "frs_main_tab_list");
                if (list == null) return;
                for (int i = 0; i < list.size(); i++) {
                    if ((Integer) XposedHelpers.getObjectField(list.get(i), "tab_type") == 14) {
                        mPosition = i;
                        XposedHelpers.setObjectField(param.thisObject, "frs_tab_default", (Integer) XposedHelpers.getObjectField(list.get(i), "tab_id"));
                        return;
                    }
                }
            }
        });
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            switch (matcher) {
                case "forum_tab_current_list":
                    if (!"com.baidu.tieba.forum.controller.TopController".equals(clazz)) return;
                    Class<?> topControllerClass = XposedHelpers.findClass(clazz, sClassLoader);
                    Method targetMethod = XposedHelpers.findMethodBestMatch(
                            topControllerClass,
                            method,
                            null,
                            XposedHelpers.findClass(clazz, sClassLoader)
                    );
                    XposedBridge.hookMethod(targetMethod, new XC_MethodHook() {
                        @Override
                        public void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Class<?> customViewPager = XposedHelpers.findClass("com.baidu.tbadk.widget.CustomViewPager", sClassLoader);
                            final Object viewPager = XposedHelpers.findFirstFieldByExactType(param.args[1].getClass(), customViewPager).get(param.args[1]);
                            XposedHelpers.callMethod(viewPager, "setCurrentItem", mPosition);
                        }
                    });
                    break;
                case "c/f/frs/page?cmd=301001&format=protobuf":
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method,
                            "com.baidu.tieba.forum.model.FrsPageRequestMessage",
                            new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    XposedHelpers.setObjectField(param.args[0], "sortType", 0);
                                }
                            });
                    break;
            }
        });
    }
}
