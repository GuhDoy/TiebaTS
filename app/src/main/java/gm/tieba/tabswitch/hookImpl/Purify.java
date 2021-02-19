package gm.tieba.tabswitch.hookImpl;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.Hook;

public class Purify extends Hook {
    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            switch (Objects.requireNonNull(map.get("rule"))) {
                case "\"c/s/splashSchedule\""://旧启动广告
                    XposedBridge.hookAllMethods(XposedHelpers.findClass(map.get("class"), lpparam.classLoader), map.get("method"), XC_MethodReplacement.returnConstant(null));
                    break;
                case "\"pic_amount\""://图片广告
                    Method[] alas = lpparam.classLoader.loadClass(map.get("class")).getDeclaredMethods();
                    for (Method ala : alas)
                        if (Arrays.toString(ala.getParameterTypes()).contains("JSONObject") && !ala.getName().equals(map.get("method")))
                            XposedBridge.hookMethod(ala, XC_MethodReplacement.returnConstant(null));
                    break;
                case "\"key_frs_dialog_ad_last_show_time\""://吧推广弹窗
                    Method[] dialogs = lpparam.classLoader.loadClass(map.get("class")).getDeclaredMethods();
                    for (Method dialog : dialogs)
                        if (dialog.getName().equals(map.get("method")) && dialog.getReturnType().toString().equals("boolean"))
                            XposedBridge.hookMethod(dialog, XC_MethodReplacement.returnConstant(true));
                    break;
                case "Lcom/baidu/tieba/R$id;->frs_ad_banner:I"://吧推广横幅
                    Method[] banners = lpparam.classLoader.loadClass(map.get("class")).getDeclaredMethods();
                    for (Method banner : banners)
                        if (Arrays.toString(banner.getParameterTypes()).startsWith("[interface java.util.List, class "))
                            XposedBridge.hookMethod(banner, new XC_MethodHook() {
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    param.args[1] = null;
                                }
                            });
                    break;
            }
        }
        //新启动广告
        XposedHelpers.findAndHookConstructor("com.baidu.mobads.vo.XAdInstanceInfo", lpparam.classLoader, JSONObject.class, XC_MethodReplacement.returnConstant(null));
        //欢迎页
        XposedHelpers.findAndHookMethod("com.baidu.tieba.launcherGuide.tblauncher.GuideActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = ((Activity) param.thisObject);
                SharedPreferences tsConfig = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                SharedPreferences sharedPreferences = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
                //如果需要执行反混淆则不跳过欢迎页
                if (tsConfig.getString("anti-confusion_version", "unknown").equals(sharedPreferences.getString("key_rate_version", "unknown"))
                        && !sharedPreferences.getString("key_rate_version", "unknown").equals("unknown")) {
                    Field[] fields = param.thisObject.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        if (field.get(param.thisObject) instanceof int[]) {
                            int[] ints = (int[]) field.get(param.thisObject);
                            for (int i = 0; i < ints.length; i++)
                                ints[i] = 0;
                        }
                    }
                }
            }
        });
        //卡片广告
        XposedHelpers.findAndHookConstructor("com.baidu.tieba.recapp.lego.model.AdCard", lpparam.classLoader, JSONObject.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = null;
            }
        });
        //新卡片广告(deprecated in 12.0.8.0)
        try {
            XposedHelpers.findAndHookConstructor("com.baidu.tieba.recapp.lego.model.CriusAdCard", lpparam.classLoader, JSONObject.class, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = null;
                }
            });
        } catch (XposedHelpers.ClassNotFoundError ignored) {
        }
        //帖子直播推荐
        XposedBridge.hookAllConstructors(XposedHelpers.findClass("tbclient.AlaLiveInfo", lpparam.classLoader), XC_MethodReplacement.returnConstant(null));
        //新帖子广告
        try {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.fun.ad.sdk.FunAdSdk", lpparam.classLoader), "init", XC_MethodReplacement.returnConstant(null));
        } catch (XposedHelpers.ClassNotFoundError ignored) {
        }
        //首页直播推荐卡片
        //查找card_home_page_ala_live_item_new，会有两个结果，查找后一个结果所在类构造函数调用
        try {
            Method[] alas = lpparam.classLoader.loadClass("com.baidu.tieba.homepage.personalize.adapter.HomePageAlaLiveThreadAdapter").getDeclaredMethods();
            for (Method ala : alas)
                if (ala.getReturnType().toString().endsWith("HomePageAlaLiveThreadViewHolder"))
                    XposedBridge.hookMethod(ala, XC_MethodReplacement.returnConstant(null));
        } catch (XposedHelpers.ClassNotFoundError ignored) {
        }
        //吧小程序
        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.baidu.tieba.frs.servicearea.ServiceAreaView", lpparam.classLoader), "setData", XC_MethodReplacement.returnConstant(null));
        //你可能感兴趣的人
        //initUI
        Method[] concerns = lpparam.classLoader.loadClass("com.baidu.tieba.homepage.concern.view.ConcernRecommendLayout").getDeclaredMethods();
        for (Method concern : concerns)
            if (Arrays.toString(concern.getParameterTypes()).equals("[]"))
                XposedBridge.hookMethod(concern, XC_MethodReplacement.returnConstant(null));
        //首页任务中心
        //R.id.task
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.NestedScrollHeader", lpparam.classLoader, "onAttachedToWindow", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field[] fields = param.thisObject.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (field.get(param.thisObject) instanceof ImageView) {
                        View view = (View) field.get(param.thisObject);
                        if (view != null) view.setVisibility(View.GONE);
                    }
                }
            }
        });
        //首页大家都在搜
        XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.view.ForumHeaderView", lpparam.classLoader, "setSearchHint", String.class, XC_MethodReplacement.returnConstant(null));
        //进吧大家都在搜
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.NestedScrollHeader", lpparam.classLoader, "setSearchHint", String.class, XC_MethodReplacement.returnConstant(null));
        //首页任务弹窗
        XposedHelpers.findAndHookMethod("com.baidu.tieba.missionCustomDialog.MissionCustomDialogActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                activity.finish();
            }
        });
        //一键签到广告
        XposedHelpers.findAndHookMethod("com.baidu.tieba.signall.SignAllForumAdvertActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                activity.finish();
            }
        });
    }
}