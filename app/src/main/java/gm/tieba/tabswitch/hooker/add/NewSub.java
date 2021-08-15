package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.content.Intent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.ReflectUtils;
import gm.tieba.tabswitch.widget.NavigationBar;

public class NewSub extends XposedContext implements IHooker {
    private Object mThreadId;
    private Object mPostId;

    public void hook() throws Throwable {
        AcRules.findRule(Constants.getMatchers().get("NewSub"), (AcRules.Callback) (rule, clazz, method) ->
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) ReflectUtils.getObjectField(param.thisObject,
                                "com.baidu.tieba.pb.pb.sub.NewSubPbActivity");
                        if (activity.getIntent().getStringExtra("st_type").equals("search_post")) {
                            new NavigationBar(param.thisObject)
                                    .addTextButton("查看主题贴", v -> startPbActivity(activity));
                        }
                    }
                }));
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", sClassLoader,
                "build", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object thread = XposedHelpers.getObjectField(param.thisObject, "thread");
                        Object post = XposedHelpers.getObjectField(param.thisObject, "post");
                        // null when post is omitted
                        if (thread != null && post != null) {
                            mThreadId = XposedHelpers.getObjectField(thread, "id");
                            mPostId = XposedHelpers.getObjectField(post, "id");
                        }
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
