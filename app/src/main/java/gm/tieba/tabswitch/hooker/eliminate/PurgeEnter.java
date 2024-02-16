package gm.tieba.tabswitch.hooker.eliminate;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import java.lang.reflect.Modifier;
import java.util.List;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.ClassMatcherHelper;
import gm.tieba.tabswitch.hooker.deobfuscation.MethodNameMatcher;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.ResIdentifierMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;

public class PurgeEnter extends XposedContext implements IHooker, Obfuscated {
    @NonNull
    @Override
    public String key() {
        return "purge_enter";
    }

    private int mInitLayoutHeight = -1;
    private final int mLayoutOffset = (int) ReflectUtils.getDimen("tbds50");
    private String mRecForumClassName, mRecForumSetNextPageMethodName;

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(
                new ResIdentifierMatcher("tbds400", "dimen", ClassMatcherHelper.usingString("enter_forum_login_tip")),
                new MethodNameMatcher("onSuccess", ClassMatcherHelper.usingString("enter_forum_login_tip"))
        );
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod(
                "com.baidu.tieba.enterForum.recforum.message.RecommendForumRespondedMessage",
                sClassLoader,
                "getRecommendForumData",
                XC_MethodReplacement.returnConstant(null));

        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            switch (matcher) {
                case "enter_forum_login_tip/dimen.tbds400":
                    mRecForumClassName = clazz;
                    mRecForumSetNextPageMethodName = method;
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            Object pbListView = ReflectUtils.getObjectField(param.thisObject, "com.baidu.tbadk.core.view.PbListView");

                            String viewConstructorName = "";
                            for (final var method : XposedHelpers.findClass("com.baidu.tbadk.core.view.PbListView", sClassLoader).getSuperclass().getDeclaredMethods()) {
                                if (method.getReturnType().toString().endsWith("View") && !Modifier.isAbstract(method.getModifiers())) {
                                    viewConstructorName = method.getName();
                                    break;
                                }
                            }
                            View view = (View) XposedHelpers.callMethod(pbListView, viewConstructorName);

                            if (view.getParent() == null) {
                                Object bdListView = ReflectUtils.getObjectField(param.thisObject, "com.baidu.adp.widget.ListView.BdListView");
                                XposedHelpers.callMethod(bdListView, "setNextPage", pbListView);
                            }

                            LinearLayout linearLayout = (LinearLayout) ReflectUtils.getObjectField(pbListView, "android.widget.LinearLayout");
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(linearLayout.getLayoutParams());

                            if (mInitLayoutHeight == -1){
                                mInitLayoutHeight = layoutParams.height + mLayoutOffset;
                            }
                            layoutParams.height = mInitLayoutHeight;
                            linearLayout.setLayoutParams(layoutParams);
                            return null;
                        }
                    });
                    break;
                case "enter_forum_login_tip/onSuccess":
                    XposedHelpers.findAndHookMethod(clazz,
                            sClassLoader,
                            method,
                            boolean.class,
                            new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                    Object enterForumRec = ReflectUtils.getObjectField(param.thisObject, mRecForumClassName);
                                    XposedHelpers.callMethod(enterForumRec, mRecForumSetNextPageMethodName);
                                    return null;
                                }
                            });
                    break;
            }
        });
    }
}
