package gm.tieba.tabswitch.hooker.eliminate;

import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.util.ReflectUtils;

public class PurgeEnter extends XposedContext implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.base.view.FlutterEnterForumDelegateStatic", sClassLoader,
                "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
        AcRules.findRule(Constants.getMatchers().get(PurgeEnter.class), (AcRules.Callback) (matcher, clazz, method) ->
                XposedBridge.hookAllConstructors(XposedHelpers.findClass(clazz, sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ReflectUtils.walkObjectFields(param.thisObject, View.class, objField -> {
                            View view = (View) objField;
                            view.setVisibility(View.GONE);
                            return false;
                        });
                    }
                }));
        // 可能感兴趣的吧
        Class<?> clazz = XposedHelpers.findClass("com.baidu.card.view.RecommendForumLayout", sClassLoader);
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterTypes().length == 0) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ReflectUtils.walkObjectFields(param.thisObject, View.class, objField -> {
                            View view = (View) objField;
                            view.setVisibility(View.INVISIBLE);
                            ViewGroup.LayoutParams lp = view.getLayoutParams();
                            lp.height = DisplayUtils.dipToPx(view.getContext(), 9);
                            return false;
                        });
                    }
                });
                break;
            }
        }
    }
}
