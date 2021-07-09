package gm.tieba.tabswitch.hooker.minus;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.XposedWrapper;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.util.ReflectUtils;

public class PurifyEnter extends XposedWrapper implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.base.view.FlutterEnterForumDelegateStatic", sClassLoader,
                "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
        AcRules.findRule(sRes.getStringArray(R.array.PurifyEnter), (AcRules.Callback) (rule, clazz, method) ->
                XposedBridge.hookAllConstructors(XposedHelpers.findClass(clazz, sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ReflectUtils.handleObjectFields(param.thisObject, View.class, objField -> {
                            View view = (View) objField;
                            view.setVisibility(View.GONE);
                            return false;
                        });
                    }
                }));
        // 可能感兴趣的吧
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        Class<?> clazz = XposedHelpers.findClass("com.baidu.card.view.RecommendForumLayout", sClassLoader);
                        Method method;
                        try {
                            method = clazz.getDeclaredMethod("initUI");
                        } catch (NoSuchMethodException e) {
                            method = clazz.getDeclaredMethod("b");
                        }
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                ReflectUtils.handleObjectFields(param.thisObject, View.class, objField -> {
                                    View view = (View) objField;
                                    view.setVisibility(View.INVISIBLE);
                                    ViewGroup.LayoutParams lp = view.getLayoutParams();
                                    lp.height = DisplayUtils.dipToPx(activity, 9);
                                    return false;
                                });
                            }
                        });
                    }
                });
    }
}
