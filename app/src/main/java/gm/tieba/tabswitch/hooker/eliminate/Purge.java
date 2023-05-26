package gm.tieba.tabswitch.hooker.eliminate;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;

public class Purge extends XposedContext implements IHooker, Obfuscated {

    @NonNull
    @Override
    public String key() {
        return "purge";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(
                new SmaliMatcher("Lcom/baidu/tieba/recapp/lego/model/AdCard;-><init>(Lorg/json/JSONObject;)V"),
                new SmaliMatcher("Lcom/baidu/tieba/lego/card/model/BaseCardInfo;-><init>(Lorg/json/JSONObject;)V"),
                new StringMatcher("pic_amount"),
                new StringMatcher("key_frs_dialog_ad_last_show_time")
        );
    }

    @Override
    public void hook() throws Throwable {
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            switch (matcher) {
                case "Lcom/baidu/tieba/recapp/lego/model/AdCard;-><init>(Lorg/json/JSONObject;)V": // 卡片广告
                case "Lcom/baidu/tieba/lego/card/model/BaseCardInfo;-><init>(Lorg/json/JSONObject;)V":
                    XposedBridge.hookAllMethods(XposedHelpers.findClass(clazz, sClassLoader), method, XC_MethodReplacement.returnConstant(null));
                    break;
                case "pic_amount": // 图片广告：必须"recom_ala_info", "app", 可选"goods_info"
                    for (final var md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                        if (Arrays.toString(md.getParameterTypes()).contains("JSONObject") && !md.getName().equals(method)) {
                            XposedBridge.hookMethod(md, XC_MethodReplacement.returnConstant(null));
                        }
                    }
                    break;
                case "key_frs_dialog_ad_last_show_time": // 吧推广弹窗
                    for (final var md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                        if (md.getName().equals(method) && md.getReturnType().equals(boolean.class)) {
                            XposedBridge.hookMethod(md, XC_MethodReplacement.returnConstant(true));
                        }
                    }
                    break;
            }
        });
        // 启动广告
        XposedHelpers.findAndHookMethod("com.baidu.adp.framework.MessageManager", sClassLoader, "findTask", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final int task = (int) param.args[0];
                if (task == 2016555 || task == 2921390) {
                    param.setResult(null);
                }
            }
        });
        // 热启动闪屏
        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.baidu.adp.framework.MessageManager",
                sClassLoader), "dispatchResponsedMessage", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final Object responsedMessage = param.args[0];
                if ((int) XposedHelpers.getObjectField(responsedMessage, "mCmd") == 2016520) {
                    param.setResult(null);
                }
            }
        });
        // 帖子底部推荐
        Class<?> clazz;
        try {
            clazz = XposedHelpers.findClass("com.baidu.tieba.pb.pb.main.AbsPbActivity", sClassLoader);
        } catch (final XposedHelpers.ClassNotFoundError e) {
            clazz = XposedHelpers.findClass("com.baidu.tieba.pb.pb.main.PbActivity", sClassLoader);
        }
        XposedHelpers.findAndHookMethod(clazz, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                final Bundle bundle = activity.getIntent().getExtras();
                final Intent intent = new Intent();
                for (final String key : bundle.keySet()) {
                    if /* 为您推荐 */ (key.equals("key_start_from")) {
                        final int startFrom = (int) bundle.get(key);
                        if (startFrom == 2 || startFrom == 3) intent.putExtra(key, 0);
                    } else if /* 浏览器打开热门推荐 */ (key.equals("key_uri")) {
                        final Uri uri = (Uri) bundle.get(key);
                        intent.putExtra("thread_id", uri.getQueryParameter("tid"));
                    } else {
                        final Object value = bundle.get(key);
                        if (value instanceof Serializable) {
                            intent.putExtra(key, (Serializable) value);
                        } else {
                            intent.putExtra(key, (Parcelable) value);
                        }
                    }
                }
                activity.setIntent(intent);
            }
        });
        // 帖子直播推荐：在 com/baidu/tieba/pb/pb/main/ 中搜索 tbclient/AlaLiveInfo
        XposedHelpers.findAndHookMethod("tbclient.AlaLiveInfo$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject, "user_info", null);
            }
        });
        // 首页直播推荐卡片：R.layout.card_home_page_ala_live_item_new
        for (final Method method : XposedHelpers.findClass("com.baidu.tieba.homepage.personalize.adapter.HomePageAlaLiveThreadAdapter", sClassLoader).getDeclaredMethods()) {
            if (method.getReturnType().toString().endsWith("HomePageAlaLiveThreadViewHolder")) {
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
            }
        }
        // 首页推荐
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final List<?> threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (threadList == null) return;
                threadList.removeIf(o -> {
                    if (XposedHelpers.getObjectField(o, "forum_info") == null) {
                        return true;
                    }

                    final Object worksInfo = XposedHelpers.getObjectField(o, "works_info");
                    return worksInfo != null && (Integer) XposedHelpers.getObjectField(worksInfo, "is_works") == 1;
                });
            }
        });
        // 吧页面
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                // 吧公告
                XposedHelpers.setObjectField(param.thisObject, "star_enter", new ArrayList<>());

                final List<?> threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (threadList == null) return;
                threadList.removeIf(o -> {
                    if (XposedHelpers.getObjectField(o, "ala_info") != null) {
                        return true;
                    }

                    final Object worksInfo = XposedHelpers.getObjectField(o, "works_info");
                    return worksInfo != null && (Integer) XposedHelpers.getObjectField(worksInfo, "is_works") == 1;
                });

                // 万人直播互动 吧友开黑组队中
                XposedHelpers.setObjectField(param.thisObject, "live_fuse_forum", new ArrayList<>());
            }
        });
        // 吧小程序
        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.baidu.tieba.frs.servicearea.ServiceAreaView", sClassLoader), "setData", XC_MethodReplacement.returnConstant(null));
        // 吧友直播
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.NavTabInfo$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "tab");
                if (list == null) return;
                list.removeIf(o -> (Integer) XposedHelpers.getObjectField(o, "tab_type") == 92);
            }
        });
        // 你可能感兴趣的人：initUI()
        final var md = ReflectUtils.findFirstMethodByExactType("com.baidu.tieba.homepage.concern.view.ConcernRecommendLayout");
        XposedBridge.hookMethod(md, XC_MethodReplacement.returnConstant(null));
        try {
            // 12.41.5.1+
            XposedHelpers.findAndHookMethod("com.baidu.tieba.feed.list.TemplateAdapter", sClassLoader, "setList", List.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    final var list = (List<?>) param.args[0];
                    list.removeIf(o -> {
                        final var type = (String) XposedHelpers.callMethod(o, "a");
                        return "sideway_card".equals(type);
                    });
                }
            });
        } catch (final XposedHelpers.ClassNotFoundError ignored) {
        }
        // 首页任务中心：R.id.task TbImageView
//        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.NestedScrollHeader", sClassLoader, "onAttachedToWindow", new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                ReflectUtils.walkObjectFields(param.thisObject, ImageView.class, objField -> {
//                    ImageView iv = (ImageView) objField;
//                    iv.setVisibility(View.GONE);
//                    return false;
//                });
//            }
//        });
        // 首页大家都在搜
        XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.view.ForumHeaderView", sClassLoader, "setSearchHint", String.class, XC_MethodReplacement.returnConstant(null));
        // 进吧大家都在搜
//        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.NestedScrollHeader", sClassLoader, "setSearchHint", String.class, XC_MethodReplacement.returnConstant(null));
        // 一键签到广告
        XposedHelpers.findAndHookMethod("com.baidu.tieba.signall.SignAllForumAdvertActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                activity.finish();
            }
        });
        // 首页推荐右侧悬浮
        for (final var method : XposedHelpers.findClass("com.baidu.tbadk.widget.RightFloatLayerView", sClassLoader).getDeclaredMethods()) {
            if (method.getParameterTypes().length == 0 && method.getReturnType() == boolean.class) {
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false));
            }
        }
    }
}
