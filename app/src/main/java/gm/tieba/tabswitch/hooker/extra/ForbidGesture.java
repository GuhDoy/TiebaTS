package gm.tieba.tabswitch.hooker.extra;

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
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.Reflect;

@SuppressLint("ClickableViewAccessibility")
public class ForbidGesture extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        AcRules.findRule(sRes.getString(R.string.ForbidGesture), new AcRules.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                XposedBridge.hookAllConstructors(XposedHelpers.findClass(clazz, sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.get(param.thisObject) instanceof RelativeLayout) {
                                RelativeLayout relativeLayout = (RelativeLayout) field.get(param.thisObject);
                                ListView listView = relativeLayout.findViewById(
                                        Reflect.getId("new_pb_list"));
                                if (listView == null) continue;
                                listView.setOnTouchListener((v, event) -> false);
                                return;
                            }
                        }
                    }
                });
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.videopb.fragment.DetailInfoAndReplyFragment", sClassLoader,
                "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup recyclerView = (ViewGroup) Reflect.getObjectField(param.thisObject,
                                "com.baidu.adp.widget.ListView.BdTypeRecyclerView");
                        recyclerView.setOnTouchListener((v, event) -> false);
                    }
                });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.pb.main.PbLandscapeListView", sClassLoader,
                "dispatchTouchEvent", MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        sClassLoader.loadClass("com.baidu.tieba.pb.pb.main.PbLandscapeListView")
                                .getDeclaredMethod("setForbidDragListener", boolean.class)
                                .invoke(param.thisObject, true);
                    }
                });
        Method method;
        try {
            method = sClassLoader.loadClass("com.baidu.tbadk.widget.DragImageView")
                    .getDeclaredMethod("getMaxScaleValue", Bitmap.class);
        } catch (NoSuchMethodException e) {
            method = sClassLoader.loadClass("com.baidu.tbadk.widget.DragImageView")
                    .getDeclaredMethod("U", Bitmap.class);
        }
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(3 * (float) param.getResult());
            }
        });
    }
}
