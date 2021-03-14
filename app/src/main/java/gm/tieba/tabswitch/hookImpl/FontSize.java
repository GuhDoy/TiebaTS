package gm.tieba.tabswitch.hookImpl;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class FontSize extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            if (Objects.equals(map.get("rule"), "Lcom/baidu/tieba/R$id;->new_pb_list:I"))
                XposedBridge.hookAllConstructors(XposedHelpers.findClass(map.get("class"), classLoader), new XC_MethodHook() {
                    @SuppressLint("ClickableViewAccessibility")
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Field[] fields = param.thisObject.getClass().getDeclaredFields();
                        for (Field field : fields) {
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
                Field[] fields = param.thisObject.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (field.get(param.thisObject).getClass().getName().equals("com.baidu.adp.widget.ListView.BdTypeRecyclerView")) {
                        ViewGroup recyclerView = (ViewGroup) field.get(param.thisObject);
                        recyclerView.setOnTouchListener((v, event) -> false);
                        return;
                    }
                }
            }
        });
    }
}