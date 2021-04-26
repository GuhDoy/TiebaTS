package gm.tieba.tabswitch.hooker;

import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.Hooker;
import gm.tieba.tabswitch.hooker.model.Rule;

public class PurifyMy extends BaseHooker implements Hooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.base.view.FlutterDelegateStatic", sClassLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
        Rule.findRule(new Rule.RuleCallBack() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) throws Throwable {
                switch (rule) {
                    case "Lcom/baidu/tieba/R$drawable;->icon_pure_topbar_store44_svg:I"://商店
                        XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, int.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                                    field.setAccessible(true);
                                    if (field.get(param.thisObject) instanceof ImageView) {
                                        ImageView imageView = (ImageView) field.get(param.thisObject);
                                        if (imageView.getId() == sClassLoader.loadClass("com.baidu.tieba.R$id").getField("person_navigation_dressup_img").getInt(null)) {
                                            imageView.setVisibility(View.GONE);
                                            return;
                                        }
                                    }
                                }
                            }
                        });
                        break;
                    case "Lcom/baidu/tieba/R$id;->function_item_bottom_divider:I"://分割线
                        for (Method md : sClassLoader.loadClass(clazz).getDeclaredMethods()) {
                            if (Arrays.toString(md.getParameterTypes()).equals("[interface com.baidu.tbadk.TbPageContext, int]") && !md.getName().equals(method)) {
                                XposedBridge.hookMethod(md, new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                        Field[] fields = param.thisObject.getClass().getDeclaredFields();
                                        for (Field field : fields) {
                                            field.setAccessible(true);
                                            if (field.get(param.thisObject) instanceof View) {
                                                View view = (View) field.get(param.thisObject);
                                                if (view.getId() == sClassLoader.loadClass("com.baidu.tieba.R$id").getField("function_item_bottom_divider").getInt(null)) {
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
                        for (Method md : sClassLoader.loadClass(clazz).getDeclaredMethods()) {
                            if (Arrays.toString(md.getParameterTypes()).equals("[class tbclient.Profile.ProfileResIdl]")) {
                                XposedBridge.hookMethod(md, new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                                            field.setAccessible(true);
                                            if (field.get(param.thisObject) instanceof ArrayList) {
                                                ArrayList<?> arrayList = (ArrayList<?>) field.get(param.thisObject);
                                                arrayList.remove(3);
                                                arrayList.remove(3);
                                                for (int j = 0; j < 11; j++) {
                                                    arrayList.remove(5);
                                                }
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
                            }
                        }
                        break;
                }
            }
        }, AntiConfusionHelper.getPurifyMyMatchers());
    }
}