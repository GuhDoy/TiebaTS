package gm.tieba.tabswitch.hooker.eliminate;

import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.ReflectUtils;

public class PurifyMy extends XposedContext implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.base.view.FlutterDelegateStatic", sClassLoader,
                "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
        AcRules.findRule(Constants.getMatchers().get(PurifyMy.class), (AcRules.Callback) (rule, clazz, method) -> {
            switch (rule) {
                case "Lcom/baidu/tieba/R$drawable;->icon_pure_topbar_store44_svg:I":// 商店
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            ReflectUtils.walkObjectFields(param.thisObject, ImageView.class, objField -> {
                                ImageView iv = (ImageView) objField;
                                if (iv.getId() == ReflectUtils.getId("person_navigation_dressup_img")) {
                                    iv.setVisibility(View.GONE);
                                    return true;
                                }
                                return false;
                            });
                        }
                    });
                    break;
                case "Lcom/baidu/tieba/R$id;->function_item_bottom_divider:I":// 分割线
                    for (Method md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                        if (Arrays.toString(md.getParameterTypes()).equals("[interface com.baidu.tbadk.TbPageContext, int]")
                                && !md.getName().equals(method)) {
                            XposedBridge.hookMethod(md, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    ReflectUtils.walkObjectFields(param.thisObject, View.class, objField -> {
                                        View v = (View) objField;
                                        if (v.getId() == ReflectUtils.getId("function_item_bottom_divider")) {
                                            v.setVisibility(View.GONE);
                                            return true;
                                        }
                                        return false;
                                    });
                                }
                            });
                        }
                    }
                    break;
                case "\"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1\"":// 我的ArrayList
                    for (Method md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                        if (Arrays.toString(md.getParameterTypes()).equals("[class tbclient.Profile.ProfileResIdl]")) {
                            XposedBridge.hookMethod(md, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    ArrayList<?> list = (ArrayList<?>) ReflectUtils.getObjectField(param.thisObject, ArrayList.class);
                                    list.removeIf((Predicate<Object>) o -> {
                                        try {
                                            ReflectUtils.getObjectField(o, "com.baidu.tbadk.core.data.UserData");
                                        } catch (NoSuchFieldError e) {
                                            return true;
                                        }
                                        try {
                                            for (Field field : o.getClass().getDeclaredFields()) {
                                                field.setAccessible(true);
                                                if (field.get(o) instanceof String) {
                                                    String type = (String) field.get(o);
                                                    if (type != null && !type.startsWith("http")
                                                            && !type.equals("我的收藏")
                                                            && !type.equals("浏览历史")
                                                            && !type.equals("服务中心")) {
                                                        return true;
                                                    }
                                                }
                                            }
                                        } catch (Throwable e) {
                                            XposedBridge.log(e);
                                        }
                                        return false;
                                    });
                                }
                            });
                        }
                    }
                    break;
            }
        });
    }
}
