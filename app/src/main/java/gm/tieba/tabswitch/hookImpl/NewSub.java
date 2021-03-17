package gm.tieba.tabswitch.hookImpl;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.Reflect;

public class NewSub extends Hook {
    private static Object threadId;
    private static Object postId;

    public static void hook(ClassLoader classLoader) throws Throwable {
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            if (Objects.equals(map.get("rule"), "Lcom/baidu/tieba/R$id;->subpb_head_user_info_root:I"))
                XposedHelpers.findAndHookMethod(map.get("class"), classLoader, map.get("method"), new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) Reflect.getObjectField(param.thisObject, "com.baidu.tieba.pb.pb.sub.NewSubPbActivity");
                        Object mNavigationBar = Reflect.getObjectField(param.thisObject, "com.baidu.tbadk.core.view.NavigationBar");
                        Class<?> ControlAlign = classLoader.loadClass("com.baidu.tbadk.core.view.NavigationBar$ControlAlign");
                        Object[] enums = ControlAlign.getEnumConstants();
                        for (Object HORIZONTAL_RIGHT : enums)
                            if (HORIZONTAL_RIGHT.toString().equals("HORIZONTAL_RIGHT")) {
                                Class<?> NavigationBar = classLoader.loadClass("com.baidu.tbadk.core.view.NavigationBar");
                                TextView textView = (TextView) NavigationBar.getDeclaredMethod("addTextButton", ControlAlign, String.class, View.OnClickListener.class)
                                        .invoke(mNavigationBar, HORIZONTAL_RIGHT, "查看主题贴", (View.OnClickListener) v -> startPbActivity(activity));
                                if (!DisplayHelper.isLightMode(activity))
                                    textView.setTextColor(Color.parseColor("#FFCBCBCC"));
                                return;
                            }
                    }
                });
        }
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object thread = XposedHelpers.getObjectField(param.thisObject, "thread");
                threadId = XposedHelpers.getObjectField(thread, "id");
                Object post = XposedHelpers.getObjectField(param.thisObject, "post");
                postId = XposedHelpers.getObjectField(post, "id");
            }
        });
    }

    //"com.baidu.tieba.pb.pb.main.PbModel", "initWithIntent"
    private static void startPbActivity(Activity activity) {
        Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.pb.pb.main.PbActivity");
        intent.putExtra("thread_id", String.valueOf(threadId));
        intent.putExtra("post_id", String.valueOf(postId));
        activity.startActivity(intent);
    }
}