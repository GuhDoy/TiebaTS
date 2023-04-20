package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.hooker.deobfuscation.ZipEntryMatcher;
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
                new ZipEntryMatcher(5580)
        );
    }

    @Override
    public void hook() throws Throwable {
        AcRules.findRule(new StringMatcher("has_show_message_tab_tips"), (matcher, clazz, method) -> {
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
                        AcRules.findRule(new ZipEntryMatcher(5580), (matcher, clazz, method) -> {
                            if (!"com.baidu.tieba.write.bottomButton.WriteThreadDelegateStatic".equals(clazz)) {
                                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, XC_MethodReplacement.returnConstant(null));
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
        if (Preferences.getBoolean("dynamic_style")) {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.baidu.adp.framework.MessageManager",
                    sClassLoader), "dispatchResponsedMessage", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    final Object responsedMessage = param.args[0];
                    if ((int) XposedHelpers.getObjectField(responsedMessage, "mCmd") == 2921551) {
                        param.setResult(null);
                    }
                }
            });
        }
    }
}
