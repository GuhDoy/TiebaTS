package gm.tieba.tabswitch.hooker.minus;

import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.ReflectUtils;

public class PurifyMy extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.base.view.FlutterDelegateStatic", sClassLoader,
                "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
        AcRules.findRule(sRes.getStringArray(R.array.PurifyMy), new AcRules.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                switch (rule) {
                    case "Lcom/baidu/tieba/R$drawable;->icon_pure_topbar_store44_svg:I"://商店
                        XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, int.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                                    field.setAccessible(true);
                                    if (field.get(param.thisObject) instanceof ImageView) {
                                        ImageView imageView = (ImageView) field.get(param.thisObject);
                                        if (imageView.getId() == ReflectUtils.getId("person_navigation_dressup_img")) {
                                            imageView.setVisibility(View.GONE);
                                            return;
                                        }
                                    }
                                }
                            }
                        });
                        break;
                    case "Lcom/baidu/tieba/R$id;->function_item_bottom_divider:I"://分割线
                        for (Method md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                            if (Arrays.toString(md.getParameterTypes()).equals("[interface com.baidu.tbadk.TbPageContext, int]")
                                    && !md.getName().equals(method)) {
                                XposedBridge.hookMethod(md, new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                        Field[] fields = param.thisObject.getClass().getDeclaredFields();
                                        for (Field field : fields) {
                                            field.setAccessible(true);
                                            if (field.get(param.thisObject) instanceof View) {
                                                View view = (View) field.get(param.thisObject);
                                                if (view.getId() == ReflectUtils.getId("function_item_bottom_divider")) {
                                                    view.setVisibility(View.GONE);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        }
                        break;
                    case "\"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1\""://我的ArrayList
                        for (Method md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                            if (Arrays.toString(md.getParameterTypes()).equals("[class tbclient.Profile.ProfileResIdl]")) {
                                XposedBridge.hookMethod(md, new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                                            field.setAccessible(true);
                                            if (field.get(param.thisObject) instanceof ArrayList) {
                                                ArrayList<?> list = (ArrayList<?>) field.get(param.thisObject);
                                                if (list == null) continue;
                                                for (int i = 0; i < list.size(); i++) {
                                                    try {
                                                        ReflectUtils.getObjectField(list.get(i), "com.baidu.tbadk.core.data.UserData");
                                                    } catch (NoSuchFieldError e) {
                                                        list.remove(i);
                                                        i--;
                                                        continue;
                                                    }

                                                    for (Field field2 : list.get(i).getClass().getDeclaredFields()) {
                                                        field2.setAccessible(true);
                                                        if (field2.get(list.get(i)) instanceof String) {
                                                            String type = (String) field2.get(list.get(i));
                                                            if (type != null && !type.startsWith("http")
                                                                    && !type.equals("我的收藏")
                                                                    && !type.equals("浏览历史")
                                                                    && !type.equals("服务中心")) {
                                                                list.remove(i);
                                                                i--;
                                                            }
                                                        }
                                                    }
                                                }
                                                return;
                                            }
                                        }
                                    }
                                });
                            }
                        }
                        break;
                }
            }
        });
    }
}
