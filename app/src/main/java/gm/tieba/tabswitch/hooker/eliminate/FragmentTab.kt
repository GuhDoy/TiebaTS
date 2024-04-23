package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import org.luckypray.dexkit.query.matchers.ClassMatcher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;

public class FragmentTab extends XposedContext implements IHooker, Obfuscated {

    @NonNull
    @Override
    public String key() {
        return "fragment_tab";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(
                new StringMatcher("has_show_message_tab_tips"),
                new SmaliMatcher("Lcom/airbnb/lottie/LottieAnimationView;->setImageResource(I)V")
                        .setBaseClassMatcher(ClassMatcher.create().usingStrings("has_show_message_tab_tips"))
        );
    }

    @Override
    public void hook() throws Throwable {
        AcRules.findRule("has_show_message_tab_tips", (matcher, clazz, method) -> {
            final var md = ReflectUtils.findFirstMethodByExactType(clazz, ArrayList.class);
            XposedBridge.hookMethod(md, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    final var tabsToRemove = new HashSet<String>();
                    if (Preferences.getBoolean("home_recommend")) {
                        tabsToRemove.add("com.baidu.tieba.homepage.framework.RecommendFrsDelegateStatic");
                    }
                    if (Preferences.getBoolean("enter_forum")) {
                        tabsToRemove.add("com.baidu.tieba.enterForum.home.EnterForumDelegateStatic");
                    }
                    if (Preferences.getBoolean("write_thread")) {
                        tabsToRemove.add("com.baidu.tieba.write.bottomButton.WriteThreadDelegateStatic");
                        AcRules.findRule("Lcom/airbnb/lottie/LottieAnimationView;->setImageResource(I)V", (matcher, clazz, method) -> {
                            Method md = XposedHelpers.findMethodExactIfExists(clazz, sClassLoader, method);
                            if (md != null) {
                                XposedBridge.hookMethod(md, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        ReflectUtils.setObjectField(param.thisObject, "com.baidu.tbadk.widget.lottie.TBLottieAnimationView", null);
                                        param.setResult(null);
                                    }
                                });
                            }
                        });
                    }
                    if (Preferences.getBoolean("im_message")) {
                        tabsToRemove.add("com.baidu.tieba.imMessageCenter.im.chat.notify.ImMessageCenterDelegateStatic");
                        tabsToRemove.add("com.baidu.tieba.immessagecenter.im.chat.notify.ImMessageCenterDelegateStatic");
                    }
                    final var list = (ArrayList<?>) param.args[0];
                    list.removeIf(tab -> tabsToRemove.contains(tab.getClass().getName()));
                }
            });
        });
    }
}
