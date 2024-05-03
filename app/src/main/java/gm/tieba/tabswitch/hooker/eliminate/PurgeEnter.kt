package gm.tieba.tabswitch.hooker.eliminate;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import org.luckypray.dexkit.query.matchers.ClassMatcher;

import java.lang.reflect.Modifier;
import java.util.List;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.MethodNameMatcher;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.ResMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;

public class PurgeEnter extends XposedContext implements IHooker, Obfuscated {
    @NonNull
    @Override
    public String key() {
        return "purge_enter";
    }

    private int mInitLayoutHeight = -1;
    private final int mLayoutOffset = (int) ReflectUtils.getDimen("tbds50");
    private String mRecForumClassName, mRecForumSetNextPageMethodName, mPbListViewInnerViewConstructorName;

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(
                new ResMatcher(ReflectUtils.getR("dimen", "tbds400"), "dimen.tbds400")
                        .setBaseClassMatcher(ClassMatcher.create().usingStrings("enter_forum_login_tip")),
                new MethodNameMatcher("onSuccess", "purge_enter_on_success")
                        .setBaseClassMatcher(ClassMatcher.create().usingStrings("enter_forum_login_tip"))
        );
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod(
                "com.baidu.tieba.enterForum.recforum.message.RecommendForumRespondedMessage",
                sClassLoader,
                "getRecommendForumData",
                XC_MethodReplacement.returnConstant(null));

        for (final var currMethod : XposedHelpers.findClass("com.baidu.tbadk.core.view.PbListView", sClassLoader).getSuperclass().getDeclaredMethods()) {
            if (currMethod.getReturnType().toString().endsWith("View") && !Modifier.isAbstract(currMethod.getModifiers())) {
                mPbListViewInnerViewConstructorName = currMethod.getName();
                break;
            }
        }

        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            switch (matcher) {
                case "dimen.tbds400":
                    mRecForumClassName = clazz;
                    mRecForumSetNextPageMethodName = method;
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            Object pbListView = ReflectUtils.getObjectField(param.thisObject, "com.baidu.tbadk.core.view.PbListView");
                            View pbListViewInnerView = (View) XposedHelpers.callMethod(pbListView, mPbListViewInnerViewConstructorName);

                            Object bdListView = ReflectUtils.getObjectField(param.thisObject, "com.baidu.adp.widget.ListView.BdListView");
                            if (pbListViewInnerView.getParent() == null) {
                                XposedHelpers.callMethod(bdListView, "setNextPage", pbListView);
                                XposedHelpers.callMethod(bdListView, "setOverScrollMode", View.OVER_SCROLL_ALWAYS);
                            }

                            LinearLayout linearLayout = (LinearLayout) ReflectUtils.getObjectField(pbListView, "android.widget.LinearLayout");
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(linearLayout.getLayoutParams());

                            if (mInitLayoutHeight == -1){
                                mInitLayoutHeight = layoutParams.height + mLayoutOffset;
                            }
                            layoutParams.height = mInitLayoutHeight;
                            linearLayout.setLayoutParams(layoutParams);

                            XposedHelpers.callMethod(bdListView, "setExOnSrollToBottomListener", (Object) null);
                            return null;
                        }
                    });
                    break;
                case "purge_enter_on_success":
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

        try {   // 12.56.4.0+ 禁用WebView进吧页
            XposedBridge.hookMethod(
                    ReflectUtils.findFirstMethodByExactReturnType("com.baidu.tieba.enterForum.helper.HybridEnterForumHelper", boolean.class),
                    XC_MethodReplacement.returnConstant(false));
        } catch (final XposedHelpers.ClassNotFoundError ignored) {}
    }
}
