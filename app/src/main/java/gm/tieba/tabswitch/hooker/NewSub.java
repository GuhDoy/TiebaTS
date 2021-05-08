package gm.tieba.tabswitch.hooker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.IHooker;
import gm.tieba.tabswitch.hooker.model.Rule;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.Reflect;

public class NewSub extends BaseHooker implements IHooker {
    private Object mThreadId;
    private Object mPostId;

    public void hook() throws Throwable {
        Rule.findRule("Lcom/baidu/tieba/R$id;->subpb_head_user_info_root:I", new Rule.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) Reflect.getObjectField(param.thisObject, "com.baidu.tieba.pb.pb.sub.NewSubPbActivity");
                        Object navigationBar = Reflect.getObjectField(param.thisObject, "com.baidu.tbadk.core.view.NavigationBar");
                        Class<?> ControlAlign = sClassLoader.loadClass("com.baidu.tbadk.core.view.NavigationBar$ControlAlign");
                        for (Object HORIZONTAL_RIGHT : ControlAlign.getEnumConstants()) {
                            if (HORIZONTAL_RIGHT.toString().equals("HORIZONTAL_RIGHT")) {
                                Class<?> NavigationBar = sClassLoader.loadClass("com.baidu.tbadk.core.view.NavigationBar");
                                TextView textView = (TextView) NavigationBar.getDeclaredMethod("addTextButton", ControlAlign, String.class, View.OnClickListener.class)
                                        .invoke(navigationBar, HORIZONTAL_RIGHT, "查看主题贴", (View.OnClickListener) v -> startPbActivity(activity));
                                if (!DisplayHelper.isLightMode(activity)) {
                                    textView.setTextColor(Color.parseColor("#FFCBCBCC"));
                                }
                                return;
                            }
                        }
                    }
                });
            }
        });
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object thread = XposedHelpers.getObjectField(param.thisObject, "thread");
                mThreadId = XposedHelpers.getObjectField(thread, "id");
                Object post = XposedHelpers.getObjectField(param.thisObject, "post");
                mPostId = XposedHelpers.getObjectField(post, "id");
            }
        });
    }

    // "com.baidu.tieba.pb.pb.main.PbModel", "initWithIntent"
    private void startPbActivity(Activity activity) {
        Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.pb.pb.main.PbActivity");
        intent.putExtra("thread_id", String.valueOf(mThreadId));
        intent.putExtra("post_id", String.valueOf(mPostId));
        activity.startActivity(intent);
    }
}
