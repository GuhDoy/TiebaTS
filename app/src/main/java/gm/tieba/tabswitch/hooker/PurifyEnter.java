package gm.tieba.tabswitch.hooker;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.Rule;
import gm.tieba.tabswitch.util.DisplayHelper;

public class PurifyEnter extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.base.view.FlutterEnterForumDelegateStatic", sClassLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
        Rule.findRule(sRes.getStringArray(R.array.PurifyEnter), new Rule.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                XposedBridge.hookAllConstructors(XposedHelpers.findClass(clazz, sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.get(param.thisObject) instanceof View) {
                                View view = (View) field.get(param.thisObject);
                                view.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }
        });
        //可能感兴趣的吧
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Method method;
                try {
                    method = sClassLoader.loadClass("com.baidu.card.view.RecommendForumLayout").getDeclaredMethod("initUI");
                } catch (NoSuchMethodException e) {
                    method = sClassLoader.loadClass("com.baidu.card.view.RecommendForumLayout").getDeclaredMethod("b");
                }
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.get(param.thisObject) instanceof View) {
                                View view = (View) field.get(param.thisObject);
                                view.setVisibility(View.INVISIBLE);
                                ViewGroup.LayoutParams lp = view.getLayoutParams();
                                lp.height = DisplayHelper.dip2Px(activity, 3);
                            }
                        }
                    }
                });
            }
        });
    }
}
