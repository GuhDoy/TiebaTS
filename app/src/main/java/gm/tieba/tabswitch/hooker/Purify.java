package gm.tieba.tabswitch.hooker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageView;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.dao.Rule;

public class Purify extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        Rule.findRule(AntiConfusionHelper.getPurifyMatchers(), new Rule.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) throws Throwable {
                switch (rule) {
                    case "\"c/s/splashSchedule\""://旧启动广告
                    case "Lcom/baidu/tieba/recapp/lego/model/AdCard;-><init>(Lorg/json/JSONObject;)V"://卡片广告
                        XposedBridge.hookAllMethods(XposedHelpers.findClass(clazz, sClassLoader), method, XC_MethodReplacement.returnConstant(null));
                        break;
                    case "\"pic_amount\""://图片广告：必须"recom_ala_info", "app", 可选"goods_info"
                        for (Method md : sClassLoader.loadClass(clazz).getDeclaredMethods()) {
                            if (Arrays.toString(md.getParameterTypes()).contains("JSONObject") && !md.getName().equals(method)) {
                                XposedBridge.hookMethod(md, XC_MethodReplacement.returnConstant(null));
                            }
                        }
                        break;
                    case "\"key_frs_dialog_ad_last_show_time\""://吧推广弹窗
                        for (Method md : sClassLoader.loadClass(clazz).getDeclaredMethods()) {
                            if (md.getName().equals(method) && md.getReturnType().toString().equals("boolean")) {
                                XposedBridge.hookMethod(md, XC_MethodReplacement.returnConstant(true));
                            }
                        }
                        break;
                    case "Lcom/baidu/tieba/R$id;->frs_ad_banner:I"://吧推广横幅
                        for (Method md : sClassLoader.loadClass(clazz).getDeclaredMethods()) {
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
                    case "Lcom/baidu/tieba/R$layout;->pb_child_title:I"://视频相关推荐
                        if (!Objects.equals(clazz, "com.baidu.tieba.pb.videopb.fragment.DetailInfoAndReplyFragment")) {
                            Method md;
                            try {
                                md = sClassLoader.loadClass("com.baidu.adp.widget.ListView.BdTypeRecyclerView").getDeclaredMethod("addAdapters", List.class);
                            } catch (NoSuchMethodException e) {
                                md = sClassLoader.loadClass("com.baidu.adp.widget.ListView.BdTypeRecyclerView").getDeclaredMethod("a", List.class);
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
                        }
                        break;
                }
            }
        });
        //启动广告
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.TbadkCoreApplication", sClassLoader, "getIsFirstUse", XC_MethodReplacement.returnConstant(true));
        XposedHelpers.findAndHookMethod("com.baidu.adp.framework.MessageManager", sClassLoader, "findTask", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int task = (int) param.args[0];
                if (task == 2016555 || task == 2921390) {
                    param.setResult(null);
                }
            }
        });
        //广告sdk
        try {
            for (Method method : sClassLoader.loadClass("com.fun.ad.sdk.FunAdSdk").getDeclaredMethods()) {
                if (method.getName().equals("init")) {
                    if (method.getReturnType().equals(boolean.class)) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true));
                    } else {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        //帖子直播推荐：在com/baidu/tieba/pb/pb/main/包搜索tbclient/AlaLiveInfo
        XposedHelpers.findAndHookMethod("tbclient.AlaLiveInfo$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject, "user_info", null);
            }
        });
        //首页直播推荐卡片：R.layout.card_home_page_ala_live_item_new
        try {
            for (Method method : sClassLoader.loadClass("com.baidu.tieba.homepage.personalize.adapter.HomePageAlaLiveThreadAdapter").getDeclaredMethods()) {
                if (method.getReturnType().toString().endsWith("HomePageAlaLiveThreadViewHolder")) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        //首页不属于任何吧的视频
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (list == null) return;
                for (int i = 0; i < list.size(); i++) {
                    if (XposedHelpers.getObjectField(list.get(i), "forum_info") == null) {
                        list.remove(i);
                        i--;
                    }
                }
            }
        });
        //欢迎页
        XposedHelpers.findAndHookMethod("com.baidu.tieba.launcherGuide.tblauncher.GuideActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.get(param.thisObject) instanceof int[]) {
                        int[] ints = (int[]) field.get(param.thisObject);
                        for (int i = 0; i < ints.length; i++) {
                            ints[i] = 0;
                        }
                    }
                }
            }
        });
        //浏览器打开热门推荐
        XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.pb.main.PbActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Bundle bundle = activity.getIntent().getExtras();
                Intent intent = new Intent();
                for (String key : bundle.keySet()) {
                    if (key.equals("key_uri")) {
                        intent.putExtra("thread_id", ((Uri) bundle.get(key)).getQueryParameter("tid"));
                    } else if (bundle.get(key) instanceof Serializable) {
                        intent.putExtra(key, (Serializable) bundle.get(key));
                    } else {
                        intent.putExtra(key, (Parcelable) bundle.get(key));
                    }
                }
                activity.setIntent(intent);
            }
        });
        //吧小程序
        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.baidu.tieba.frs.servicearea.ServiceAreaView", sClassLoader), "setData", XC_MethodReplacement.returnConstant(null));
        //吧友直播
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.NavTabInfo$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "tab");
                if (list == null) return;
                for (int i = 0; i < list.size(); i++) {
                    if ((int) XposedHelpers.getObjectField(list.get(i), "tab_type") == 92) {
                        list.remove(i);
                        return;
                    }
                }
            }
        });
        //吧公告
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject, "star_enter", new ArrayList<>());
            }
        });
        //你可能感兴趣的人：initUI()
        for (Method method : sClassLoader.loadClass("com.baidu.tieba.homepage.concern.view.ConcernRecommendLayout").getDeclaredMethods()) {
            if (Arrays.toString(method.getParameterTypes()).equals("[]")) {
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
            }
        }
        //首页任务中心：R.id.task
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.NestedScrollHeader", sClassLoader, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.get(param.thisObject) instanceof ImageView) {
                        ImageView imageView = (ImageView) field.get(param.thisObject);
                        imageView.setVisibility(View.GONE);
                        return;
                    }
                }
            }
        });
        //首页大家都在搜
        XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.view.ForumHeaderView", sClassLoader, "setSearchHint", String.class, XC_MethodReplacement.returnConstant(null));
        //进吧大家都在搜
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.NestedScrollHeader", sClassLoader, "setSearchHint", String.class, XC_MethodReplacement.returnConstant(null));
        //首页任务弹窗
        XposedHelpers.findAndHookMethod("com.baidu.tieba.missionCustomDialog.MissionCustomDialogActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                activity.finish();
            }
        });
        //一键签到广告
        XposedHelpers.findAndHookMethod("com.baidu.tieba.signall.SignAllForumAdvertActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                activity.finish();
            }
        });
    }
}
