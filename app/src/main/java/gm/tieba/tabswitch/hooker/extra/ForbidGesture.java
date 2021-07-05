package gm.tieba.tabswitch.hooker.extra;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.ReflectUtils;

@SuppressLint("ClickableViewAccessibility")
public class ForbidGesture extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        // 帖子字号
        AcRules.findRule(sRes.getString(R.string.ForbidGesture), (AcRules.Callback) (rule, clazz, method) ->
                XposedBridge.hookAllConstructors(XposedHelpers.findClass(clazz, sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ReflectUtils.handleObjectFields(param.thisObject, RelativeLayout.class, objField -> {
                            RelativeLayout rl = (RelativeLayout) ReflectUtils.getObjectField(param.thisObject, RelativeLayout.class);
                            ListView list = rl.findViewById(ReflectUtils.getId("new_pb_list"));
                            if (list != null) {
                                list.setOnTouchListener((v, event) -> false);
                                return true;
                            }
                            return false;
                        });
                    }
                }));
        // 视频帖字号
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.videopb.fragment.DetailInfoAndReplyFragment", sClassLoader,
                "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ViewGroup recyclerView = (ViewGroup) ReflectUtils.getObjectField(param.thisObject,
                                "com.baidu.adp.widget.ListView.BdTypeRecyclerView");
                        recyclerView.setOnTouchListener((v, event) -> false);
                    }
                });
        // 帖子进吧
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.pb.main.PbLandscapeListView", sClassLoader,
                "dispatchTouchEvent", MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        XposedHelpers.callMethod(param.thisObject, "setForbidDragListener", true);
                    }
                });
        // 图片缩放倍数
        Class<?> clazz = XposedHelpers.findClass("com.baidu.tbadk.widget.DragImageView", sClassLoader);
        Method method;
        try {
            method = clazz.getDeclaredMethod("getMaxScaleValue", Bitmap.class);
        } catch (NoSuchMethodException e) {
            method = clazz.getDeclaredMethod("U", Bitmap.class);
        }
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(3 * (float) param.getResult());
            }
        });
    }
}
