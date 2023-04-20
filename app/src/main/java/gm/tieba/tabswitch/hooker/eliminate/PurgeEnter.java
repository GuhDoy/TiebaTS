package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

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
import gm.tieba.tabswitch.hooker.deobfuscation.StringResMatcher;

public class PurgeEnter extends XposedContext implements IHooker, Obfuscated {

    private Object mRecommendHotForumTitle;

    @NonNull
    @Override
    public String key() {
        return "purge_enter";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(
                new StringResMatcher("热门吧精选"),
                new StringMatcher("c13378")
        );
    }

    @Override
    public void hook() throws Throwable {
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            switch (matcher) {
                case "热门吧精选":
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            mRecommendHotForumTitle = param.getResult();
                        }
                    });
                    break;
                case "c13378":
                    for (final var md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                        final var paramTypes = md.getParameterTypes();
                        if (paramTypes.length == 2 && paramTypes[0] == List.class) {
                            XposedBridge.hookMethod(md, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                                    if (mRecommendHotForumTitle != null) {
                                        final var list = (List<?>) param.args[0];
                                        final var index = list.indexOf(mRecommendHotForumTitle);
                                        if (index != -1) {
                                            while (list.size() > index) {
                                                list.remove(index);
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                    break;
            }
        });
    }
}
