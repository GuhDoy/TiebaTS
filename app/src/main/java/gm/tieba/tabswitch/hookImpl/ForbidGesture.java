package gm.tieba.tabswitch.hookImpl;

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
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class ForbidGesture extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            if (Objects.equals(map.get("rule"), "Lcom/baidu/tieba/R$id;->new_pb_list:I"))
                XposedBridge.hookAllConstructors(XposedHelpers.findClass(map.get("class"), classLoader), new XC_MethodHook() {
                    @SuppressLint("ClickableViewAccessibility")
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.get(param.thisObject) instanceof RelativeLayout) {
                                RelativeLayout relativeLayout = (RelativeLayout) field.get(param.thisObject);
                                ListView listView = relativeLayout.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("new_pb_list").getInt(null));
                                if (listView == null) continue;
                                listView.setOnTouchListener((v, event) -> false);
                                return;
                            }
                        }
                    }
                });
        }
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.videopb.fragment.DetailInfoAndReplyFragment", classLoader, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
            @SuppressLint("ClickableViewAccessibility")
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
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.pb.main.PbLandscapeListView", classLoader, "dispatchTouchEvent", MotionEvent.class, new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                classLoader.loadClass("com.baidu.tieba.pb.pb.main.PbLandscapeListView")
                        .getDeclaredMethod("setForbidDragListener", boolean.class).invoke(param.thisObject, true);
            }
        });
        Method method;
        try {
            method = classLoader.loadClass("com.baidu.tbadk.widget.DragImageView").getDeclaredMethod("getMaxScaleValue", Bitmap.class);
        } catch (NoSuchMethodException e) {
            method = classLoader.loadClass("com.baidu.tbadk.widget.DragImageView").getDeclaredMethod("U", Bitmap.class);
        }
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                float mMaxScale = (float) param.getResult();
                param.setResult(3 * mMaxScale);
            }
        });
    }
}