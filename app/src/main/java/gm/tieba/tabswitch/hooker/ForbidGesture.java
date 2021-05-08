package gm.tieba.tabswitch.hooker;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.IHooker;
import gm.tieba.tabswitch.hooker.model.Rule;

@SuppressLint("ClickableViewAccessibility")
public class ForbidGesture extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        Rule.findRule("Lcom/baidu/tieba/R$id;->new_pb_list:I", new Rule.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                XposedBridge.hookAllConstructors(XposedHelpers.findClass(clazz, sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.get(param.thisObject) instanceof RelativeLayout) {
                                RelativeLayout relativeLayout = (RelativeLayout) field.get(param.thisObject);
                                ListView listView = relativeLayout.findViewById(sClassLoader.loadClass("com.baidu.tieba.R$id").getField("new_pb_list").getInt(null));
                                if (listView == null) continue;
                                listView.setOnTouchListener((v, event) -> false);
                                return;
                            }
                        }
                    }
                });
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.videopb.fragment.DetailInfoAndReplyFragment", sClassLoader, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.get(param.thisObject).getClass().getName().equals("com.baidu.adp.widget.ListView.BdTypeRecyclerView")) {
                        ViewGroup recyclerView = (ViewGroup) field.get(param.thisObject);
                        recyclerView.setOnTouchListener((v, event) -> false);
                        return;
                    }
                }
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.pb.main.PbLandscapeListView", sClassLoader, "dispatchTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                sClassLoader.loadClass("com.baidu.tieba.pb.pb.main.PbLandscapeListView")
                        .getDeclaredMethod("setForbidDragListener", boolean.class).invoke(param.thisObject, true);
            }
        });
        Method method;
        try {
            method = sClassLoader.loadClass("com.baidu.tbadk.widget.DragImageView").getDeclaredMethod("getMaxScaleValue", Bitmap.class);
        } catch (NoSuchMethodException e) {
            method = sClassLoader.loadClass("com.baidu.tbadk.widget.DragImageView").getDeclaredMethod("U", Bitmap.class);
        }
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(3 * (float) param.getResult());
            }
        });
    }
}
