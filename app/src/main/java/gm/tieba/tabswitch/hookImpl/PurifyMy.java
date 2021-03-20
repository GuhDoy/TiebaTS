package gm.tieba.tabswitch.hookImpl;

import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class PurifyMy extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterDelegateStatic", classLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            switch (Objects.requireNonNull(map.get("rule"))) {
                case "Lcom/baidu/tieba/R$drawable;->icon_pure_topbar_store44_svg:I"://商店
                    XposedHelpers.findAndHookMethod(map.get("class"), classLoader, map.get("method"), int.class, new XC_MethodHook() {
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                                field.setAccessible(true);
                                if (field.get(param.thisObject) instanceof ImageView) {
                                    ImageView imageView = (ImageView) field.get(param.thisObject);
                                    if (imageView.getId() == classLoader.loadClass("com.baidu.tieba.R$id").getField("person_navigation_dressup_img").getInt(null)) {
                                        imageView.setVisibility(View.GONE);
                                        return;
                                    }
                                }
                            }
                        }
                    });
                    break;
                case "Lcom/baidu/tieba/R$id;->function_item_bottom_divider:I"://分割线
                    for (Method method : classLoader.loadClass(map.get("class")).getDeclaredMethods())
                        if (Arrays.toString(method.getParameterTypes()).equals("[interface com.baidu.tbadk.TbPageContext, int]") && !method.getName().equals(map.get("method")))
                            XposedBridge.hookMethod(method, new XC_MethodHook() {
                                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                    Field[] fields = param.thisObject.getClass().getDeclaredFields();
                                    for (Field field : fields) {
                                        field.setAccessible(true);
                                        if (field.get(param.thisObject) instanceof View) {
                                            View view = (View) field.get(param.thisObject);
                                            if (view.getId() == classLoader.loadClass("com.baidu.tieba.R$id").getField("function_item_bottom_divider").getInt(null)) {
                                                view.setVisibility(View.GONE);
                                                return;
                                            }
                                        }
                                    }
                                }
                            });
                    break;
                case "\"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1\""://我的ArrayList
                    for (Method method : classLoader.loadClass(map.get("class")).getDeclaredMethods())
                        if (Arrays.toString(method.getParameterTypes()).equals("[class tbclient.Profile.ProfileResIdl]"))
                            XposedBridge.hookMethod(method, new XC_MethodHook() {
                                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                    for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                                        field.setAccessible(true);
                                        if (field.get(param.thisObject) instanceof ArrayList) {
                                            ArrayList<?> arrayList = (ArrayList<?>) field.get(param.thisObject);
                                            arrayList.remove(3);
                                            arrayList.remove(3);
                                            for (int j = 0; j < 11; j++)
                                                arrayList.remove(5);
                                            Iterator<?> iterator = arrayList.iterator();
                                            for (int k = 0; k < 6; k++) iterator.next();
                                            while (iterator.hasNext()) {
                                                iterator.next();
                                                iterator.remove();
                                            }
                                            return;
                                        }
                                    }
                                }
                            });
                    break;
            }
        }
    }
}