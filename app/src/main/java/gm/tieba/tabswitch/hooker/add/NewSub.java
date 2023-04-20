package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;
import gm.tieba.tabswitch.widget.NavigationBar;

public class NewSub extends XposedContext implements IHooker, Obfuscated {

    @NonNull
    @Override
    public String key() {
        return "new_sub";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(new StringMatcher("c0132"));
    }

    private Object mThreadId;
    private Object mPostId;

    @Override
    public void hook() throws Throwable {
        AcRules.findRule(matchers(), (matcher, clazz, method) ->
                XposedBridge.hookAllConstructors(XposedHelpers.findClass(clazz, sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        try {
                            final var activity = (Activity) ReflectUtils.getObjectField(param.thisObject,
                                    "com.baidu.tieba.pb.pb.sub.NewSubPbActivity");
                            if (activity.getIntent().getStringExtra("st_type").equals("search_post")) {
                                new NavigationBar(param.thisObject)
                                        .addTextButton("查看主题贴", v -> startPbActivity(activity));
                            }
                        } catch (final NoSuchFieldError ignored) {
                        }
                    }
                }));
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", sClassLoader,
                "build", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                        final var thread = XposedHelpers.getObjectField(param.thisObject, "thread");
                        final var post = XposedHelpers.getObjectField(param.thisObject, "post");
                        // null when post is omitted
                        if (thread != null && post != null) {
                            mThreadId = XposedHelpers.getObjectField(thread, "id");
                            mPostId = XposedHelpers.getObjectField(post, "id");
                        }
                    }
                });
    }

    // "com.baidu.tieba.pb.pb.main.PbModel", "initWithIntent"
    private void startPbActivity(final Activity activity) {
        final var intent = new Intent().setClassName(activity, "com.baidu.tieba.pb.pb.main.PbActivity");
        intent.putExtra("thread_id", String.valueOf(mThreadId));
        intent.putExtra("post_id", String.valueOf(mPostId));
        activity.startActivity(intent);
    }
}
