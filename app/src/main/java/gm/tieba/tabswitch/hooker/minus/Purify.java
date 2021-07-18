package gm.tieba.tabswitch.hooker.minus;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageView;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.ReflectUtils;

public class Purify extends XposedContext implements IHooker {
    public void hook() throws Throwable {
        AcRules.findRule(sRes.getStringArray(R.array.Purify), (AcRules.Callback) (rule, clazz, method) -> {
            switch (rule) {
                case "\"c/s/splashSchedule\"":// 旧启动广告
                case "Lcom/baidu/tieba/recapp/lego/model/AdCard;-><init>(Lorg/json/JSONObject;)V":// 卡片广告
                    XposedBridge.hookAllMethods(XposedHelpers.findClass(clazz, sClassLoader), method, XC_MethodReplacement.returnConstant(null));
                    break;
                case "\"pic_amount\"":// 图片广告：必须"recom_ala_info", "app", 可选"goods_info"
                    for (Method md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                        if (Arrays.toString(md.getParameterTypes()).contains("JSONObject") && !md.getName().equals(method)) {
                            XposedBridge.hookMethod(md, XC_MethodReplacement.returnConstant(null));
                        }
                    }
                    break;
                case "\"key_frs_dialog_ad_last_show_time\"":// 吧推广弹窗
                    for (Method md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                        if (md.getName().equals(method) && md.getReturnType().toString().equals("boolean")) {
                            XposedBridge.hookMethod(md, XC_MethodReplacement.returnConstant(true));
                        }
                    }
                    break;
                case "Lcom/baidu/tieba/R$id;->frs_ad_banner:I":// 吧推广横幅
                    for (Method md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                        if (Arrays.toString(md.getParameterTypes()).startsWith("[interface java.util.List, class ")) {
                            XposedBridge.hookMethod(md, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    param.args[1] = null;
                                }
                            });
                        }
                    }
                    break;
                case "Lcom/baidu/tieba/R$layout;->pb_child_title:I":// 视频相关推荐
                    if (!("com.baidu.tieba.pb.videopb.fragment.DetailInfoAndReplyFragment").equals(clazz)) {
                        Class<?> clazz2 = XposedHelpers.findClass("com.baidu.adp.widget.ListView.BdTypeRecyclerView", sClassLoader);
                        try {
                            Method md;
                            try {
                                md = clazz2.getDeclaredMethod("addAdapters", List.class);
                            } catch (NoSuchMethodException e) {
                                md = clazz2.getDeclaredMethod("a", List.class);
                            }
                            XposedBridge.hookMethod(md, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    List<?> list = (List<?>) param.args[0];
                                    for (int i = 0; i < list.size(); i++) {
                                        if (list.get(i) != null && Objects.equals(clazz, list.get(i).getClass().getName())) {
                                            list.remove(i);
                                            list.remove(i);
                                            return;
                                        }
                                    }
                                }
                            });
                        } catch (NoSuchMethodException e) {
                            XposedBridge.log(e);
                        }
                    }
                    break;
            }
        });
        // 启动广告
        XposedHelpers.findAndHookMethod("com.baidu.adp.framework.MessageManager", sClassLoader, "findTask", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int task = (int) param.args[0];
                if (task == 2016555 || task == 2921390) {
                    param.setResult(null);
                }
            }
        });
        // 热启动闪屏
        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.baidu.adp.framework.MessageManager",
                sClassLoader), "dispatchResponsedMessage", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object responsedMessage = param.args[0];
                if ((int) XposedHelpers.getObjectField(responsedMessage, "mCmd") == 2016520) {
                    param.setResult(null);
                }
            }
        });
        // 广告sdk
        for (Method method : XposedHelpers.findClass("com.fun.ad.sdk.FunAdSdk", sClassLoader).getDeclaredMethods()) {
            if (method.getName().equals("init")) {
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(
                        method.getReturnType().equals(boolean.class) ? true : null));
            }
        }
        // 帖子底部推荐
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.pb.main.PbActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Bundle bundle = activity.getIntent().getExtras();
                Intent intent = new Intent();
                for (String key : bundle.keySet()) {
                    if /* 为您推荐 */ (key.equals("key_start_from")) {
                        int startFrom = (int) bundle.get(key);
                        if (startFrom == 2 || startFrom == 3) intent.putExtra(key, 0);
                    } else if /* 浏览器打开热门推荐 */ (key.equals("key_uri")) {
                        Uri uri = (Uri) bundle.get(key);
                        intent.putExtra("thread_id", uri.getQueryParameter("tid"));
                    } else {
                        Object value = bundle.get(key);
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
        // 帖子直播推荐：在com/baidu/tieba/pb/pb/main/包搜索tbclient/AlaLiveInfo
        XposedHelpers.findAndHookMethod("tbclient.AlaLiveInfo$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject, "user_info", null);
            }
        });
        // 首页直播推荐卡片：R.layout.card_home_page_ala_live_item_new
        for (Method method : XposedHelpers.findClass("com.baidu.tieba.homepage.personalize.adapter.HomePageAlaLiveThreadAdapter", sClassLoader).getDeclaredMethods()) {
            if (method.getReturnType().toString().endsWith("HomePageAlaLiveThreadViewHolder")) {
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
            }
        }
        // 首页推荐
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (threadList == null) return;
                threadList.removeIf((Predicate<Object>) o -> {
                    if (XposedHelpers.getObjectField(o, "forum_info") == null) {
                        return true;
                    }

                    Object worksInfo = XposedHelpers.getObjectField(o, "works_info");
                    return worksInfo != null && (Integer) XposedHelpers.getObjectField(worksInfo, "is_works") == 1;
                });
            }
        });
        // 吧页面
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // 吧公告
                XposedHelpers.setObjectField(param.thisObject, "star_enter", new ArrayList<>());

                List<?> threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (threadList == null) return;
                threadList.removeIf((Predicate<Object>) o -> {
                    if (XposedHelpers.getObjectField(o, "ala_info") != null) {
                        return true;
                    }

                    Object worksInfo = XposedHelpers.getObjectField(o, "works_info");
                    return worksInfo != null && (Integer) XposedHelpers.getObjectField(worksInfo, "is_works") == 1;
                });
            }
        });
        // 吧小程序
        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.baidu.tieba.frs.servicearea.ServiceAreaView", sClassLoader), "setData", XC_MethodReplacement.returnConstant(null));
        // 吧友直播
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.NavTabInfo$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "tab");
                if (list == null) return;
                list.removeIf((Predicate<Object>) o -> (Integer) XposedHelpers.getObjectField(o, "tab_type") == 92);
            }
        });
        // 你可能感兴趣的人：initUI()
        for (Method method : XposedHelpers.findClass("com.baidu.tieba.homepage.concern.view.ConcernRecommendLayout", sClassLoader).getDeclaredMethods()) {
            if (Arrays.toString(method.getParameterTypes()).equals("[]")) {
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
            }
        }
        // 首页任务中心：R.id.task TbImageView
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.NestedScrollHeader", sClassLoader, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ReflectUtils.handleObjectFields(param.thisObject, ImageView.class, objField -> {
                    ImageView iv = (ImageView) objField;
                    iv.setVisibility(View.GONE);
                    return false;
                });
            }
        });
        // 首页大家都在搜
        XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.view.ForumHeaderView", sClassLoader, "setSearchHint", String.class, XC_MethodReplacement.returnConstant(null));
        // 进吧大家都在搜
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.NestedScrollHeader", sClassLoader, "setSearchHint", String.class, XC_MethodReplacement.returnConstant(null));
        // 首页任务弹窗
        XposedHelpers.findAndHookMethod("com.baidu.tieba.missionCustomDialog.MissionCustomDialogActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                activity.finish();
            }
        });
        // 一键签到广告
        XposedHelpers.findAndHookMethod("com.baidu.tieba.signall.SignAllForumAdvertActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                activity.finish();
            }
        });
        // 欢迎页
        XposedHelpers.findAndHookMethod("com.baidu.tieba.launcherGuide.tblauncher.GuideActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ReflectUtils.handleObjectFields(param.thisObject, int[].class, objField -> {
                    int[] ints = (int[]) objField;
                    for (int i = 0; i < ints.length; i++) {
                        ints[i] = 0;
                    }
                    return false;
                });
            }
        });
    }
}
