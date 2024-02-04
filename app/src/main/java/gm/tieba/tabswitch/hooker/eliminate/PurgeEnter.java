package gm.tieba.tabswitch.hooker.eliminate;

import android.app.AndroidAppHelper;
import android.content.res.Resources;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import java.lang.reflect.Modifier;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;

public class PurgeEnter extends XposedContext implements IHooker, Obfuscated {
    @NonNull
    @Override
    public String key() {
        return "purge_enter";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(
                new StringMatcher("c/f/forum/getRecommendForumData"),
                new StringMatcher("enter_forum_login_tip"),
                new SmaliMatcher("Lcom/baidu/adp/widget/ListView/BdListView;->setNextPage(Lcom/baidu/tieba/di;)V")
        );
    }

    private String mEnterForumAdViewClassName;
    private int mInitLayoutHeight = -1;
    private final Resources currentRes = AndroidAppHelper.currentApplication().getApplicationContext().getResources();
    private final int mLayoutOffset = currentRes.getDimensionPixelSize(currentRes.getIdentifier("tbds50", "dimen", "com.baidu.tieba"));

    @Override
    public void hook() throws Throwable {
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            switch (matcher) {
                case "c/f/forum/getRecommendForumData":
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, XC_MethodReplacement.returnConstant(null));
                    break;
                case "enter_forum_login_tip":
                    mEnterForumAdViewClassName = clazz;
                    break;
                case "Lcom/baidu/adp/widget/ListView/BdListView;->setNextPage(Lcom/baidu/tieba/di;)V":
                    if (clazz.equals(mEnterForumAdViewClassName)) {
                        if (XposedHelpers.findMethodExact(clazz, sClassLoader, method).getParameterTypes().length == 0) {
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
                        }
                    }
                    break;
            }
        });
    }
}
