package gm.tieba.tabswitch.hookImpl;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.util.DisplayHelper;

public class PurifyEnter extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        try {
            XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterEnterForumDelegateStatic", classLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
        } catch (XposedHelpers.ClassNotFoundError ignored) {
        }
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            switch (Objects.requireNonNull(map.get("rule"))) {
                case "Lcom/baidu/tieba/R$id;->square_background:I"://吧广场
                case "Lcom/baidu/tieba/R$id;->create_bar_container:I"://创建自己的吧
                    XposedBridge.hookAllConstructors(XposedHelpers.findClass(map.get("class"), classLoader), new XC_MethodHook() {
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Field[] fields = param.thisObject.getClass().getDeclaredFields();
                            for (Field field : fields) {
                                field.setAccessible(true);
                                if (field.get(param.thisObject) instanceof View) {
                                    View view = (View) field.get(param.thisObject);
                                    view.setVisibility(View.GONE);
                                }
                            }
                        }
                    });
                    break;
            }
        }
        //可能感兴趣的吧
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Method method = null;
                try {
                    method = classLoader.loadClass("com.baidu.card.view.RecommendForumLayout").getDeclaredMethod("initUI");
                } catch (NoSuchMethodException e) {
                    Method[] methods = classLoader.loadClass("com.baidu.card.view.RecommendForumLayout").getDeclaredMethods();
                    for (Method md : methods)
                        if (md.getName().equals("a"))
                            method = md;
                }
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Field[] fields = param.thisObject.getClass().getDeclaredFields();
                        for (Field field : fields) {
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