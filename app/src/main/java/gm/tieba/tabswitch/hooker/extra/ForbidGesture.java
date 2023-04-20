package gm.tieba.tabswitch.hooker.extra;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringResMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;

public class ForbidGesture extends XposedContext implements IHooker, Obfuscated {

    @NonNull
    @Override
    public String key() {
        return "forbid_gesture";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(new StringResMatcher("特大号字体"));
    }

    @Override
    public void hook() throws Throwable {
        // 帖子字号
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, "c", XC_MethodReplacement.returnConstant(null));
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, "d", XC_MethodReplacement.returnConstant(null));
                }
        );
        // 视频帖字号
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.videopb.fragment.DetailInfoAndReplyFragment", sClassLoader,
                "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        final ViewGroup recyclerView = (ViewGroup) ReflectUtils.getObjectField(param.thisObject,
                                "com.baidu.adp.widget.ListView.BdTypeRecyclerView");
                        recyclerView.setOnTouchListener((v, event) -> false);
                    }
                });
        // 帖子进吧
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.pb.main.PbLandscapeListView", sClassLoader,
                "dispatchTouchEvent", MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                        XposedHelpers.callMethod(param.thisObject, "setForbidDragListener", true);
                    }
                });
        // 图片缩放倍数
        final Class<?> clazz = XposedHelpers.findClass("com.baidu.tbadk.widget.DragImageView", sClassLoader);
        Method method;
        try {
            method = clazz.getDeclaredMethod("getMaxScaleValue", Bitmap.class);
        } catch (final NoSuchMethodException e) {
            method = clazz.getDeclaredMethod("U", Bitmap.class);
        }
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(3 * (float) param.getResult());
            }
        });
    }
}
