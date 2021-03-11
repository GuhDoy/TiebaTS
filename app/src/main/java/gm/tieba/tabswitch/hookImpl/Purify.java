package gm.tieba.tabswitch.hookImpl;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class Purify extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            switch (Objects.requireNonNull(map.get("rule"))) {
                case "\"c/s/splashSchedule\""://旧启动广告
                case "\"custom_ext_data\""://sdk启动广告：搜索"bes_ad_id"，查找所在方法调用
                    XposedBridge.hookAllMethods(XposedHelpers.findClass(map.get("class"), classLoader), map.get("method"), XC_MethodReplacement.returnConstant(null));
                    break;
                case "\"pic_amount\""://图片广告：必须"recom_ala_info", "app", 可选"goods_info"
                    Method[] alas = classLoader.loadClass(map.get("class")).getDeclaredMethods();
                    for (Method ala : alas)
                        if (Arrays.toString(ala.getParameterTypes()).contains("JSONObject") && !ala.getName().equals(map.get("method")))
                            XposedBridge.hookMethod(ala, XC_MethodReplacement.returnConstant(null));
                    break;
                    /*
                case "Lcom/baidu/tieba/recapp/lego/model/AdCard;-><init>(Lorg/json/JSONObject;)V"://卡片广告
                    XposedHelpers.findAndHookMethod(map.get("class"), classLoader, map.get("method"), JSONObject.class,int.class, XC_MethodReplacement.returnConstant(null));
                    break;
                     */
                case "\"key_frs_dialog_ad_last_show_time\""://吧推广弹窗
                    Method[] dialogs = classLoader.loadClass(map.get("class")).getDeclaredMethods();
                    for (Method dialog : dialogs)
                        if (dialog.getName().equals(map.get("method")) && dialog.getReturnType().toString().equals("boolean"))
                            XposedBridge.hookMethod(dialog, XC_MethodReplacement.returnConstant(true));
                    break;
                case "Lcom/baidu/tieba/R$id;->frs_ad_banner:I"://吧推广横幅
                    Method[] banners = classLoader.loadClass(map.get("class")).getDeclaredMethods();
                    for (Method banner : banners)
                        if (Arrays.toString(banner.getParameterTypes()).startsWith("[interface java.util.List, class "))
                            XposedBridge.hookMethod(banner, new XC_MethodHook() {
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    param.args[1] = null;
                                }
                            });
                    break;
                case "Lcom/baidu/tieba/R$string;->mark_like:I"://关注作者追帖更简单
                    XposedBridge.hookAllMethods(XposedHelpers.findClass(map.get("class"), classLoader), map.get("method"), new XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[0].getClass().getName().equals("com.baidu.tbadk.core.data.MetaData"))
                                param.args[0] = null;
                        }
                    });
                    break;
            }
        }
        //启动广告
        XposedHelpers.findAndHookConstructor("com.baidu.mobads.vo.XAdInstanceInfo", classLoader, JSONObject.class, XC_MethodReplacement.returnConstant(null));
        //卡片广告
        XposedHelpers.findAndHookConstructor("com.baidu.tieba.recapp.lego.model.AdCard", classLoader, JSONObject.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = null;
            }
        });
        //广告sdk："gdt_plugin.jar", "/api/ad/union/sdk/get_ads/"
        try {
            Method[] funs = classLoader.loadClass("com.fun.ad.sdk.FunAdSdk").getDeclaredMethods();
            for (Method fun : funs)
                if (fun.getName().equals("init"))
                    if (fun.getReturnType().toString().equals("void"))
                        XposedBridge.hookMethod(fun, XC_MethodReplacement.returnConstant(null));
                    else XposedBridge.hookMethod(fun, XC_MethodReplacement.returnConstant(true));
        } catch (XposedHelpers.ClassNotFoundError ignored) {
        }
        //卡片广告
        XposedHelpers.findAndHookConstructor("com.baidu.tieba.recapp.lego.model.AdCard", classLoader, JSONObject.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = null;
            }
        });
        //新卡片广告(deprecated in 12.0.8.0)
        try {
            XposedHelpers.findAndHookConstructor("com.baidu.tieba.recapp.lego.model.CriusAdCard", classLoader, JSONObject.class, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = null;
                }
            });
        } catch (XposedHelpers.ClassNotFoundError ignored) {
        }
        //帖子直播推荐：在com/baidu/tieba/pb/pb/main/包搜索tbclient/AlaLiveInfo
        XposedHelpers.findAndHookMethod("tbclient.AlaLiveInfo$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field field = param.thisObject.getClass().getDeclaredField("user_info");
                field.setAccessible(true);
                field.set(param.thisObject, null);
            }
        });
        //首页直播推荐卡片：搜索card_home_page_ala_live_item_new，会有两个结果，查找后一个结果所在类构造函数调用
        try {
            Method[] alas = classLoader.loadClass("com.baidu.tieba.homepage.personalize.adapter.HomePageAlaLiveThreadAdapter").getDeclaredMethods();
            for (Method ala : alas)
                if (ala.getReturnType().toString().endsWith("HomePageAlaLiveThreadViewHolder"))
                    XposedBridge.hookMethod(ala, XC_MethodReplacement.returnConstant(null));
        } catch (XposedHelpers.ClassNotFoundError ignored) {
        }
        //首页不属于任何吧的视频
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field field = param.thisObject.getClass().getDeclaredField("thread_list");
                field.setAccessible(true);
                List<?> list = (List<?>) field.get(param.thisObject);
                if (list == null) return;
                for (int i = 0; i < list.size(); i++)
                    if (!list.get(i).toString().contains(", forum_info=")) {
                        list.remove(i);
                        i--;
                    }
            }
        });
        //欢迎页
        XposedHelpers.findAndHookMethod("com.baidu.tieba.launcherGuide.tblauncher.GuideActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = ((Activity) param.thisObject);
                if (AntiConfusionHelper.getLostList().size() != 0 || AntiConfusionHelper.isDexChanged(activity))
                    return;
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
        });
        //吧小程序
        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.baidu.tieba.frs.servicearea.ServiceAreaView", classLoader), "setData", XC_MethodReplacement.returnConstant(null));
        //你可能感兴趣的人：initUI
        Method[] concerns = classLoader.loadClass("com.baidu.tieba.homepage.concern.view.ConcernRecommendLayout").getDeclaredMethods();
        for (Method concern : concerns)
            if (Arrays.toString(concern.getParameterTypes()).equals("[]"))
                XposedBridge.hookMethod(concern, XC_MethodReplacement.returnConstant(null));
        //首页任务中心：R.id.task
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.NestedScrollHeader", classLoader, "onAttachedToWindow", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field[] fields = param.thisObject.getClass().getDeclaredFields();
                for (Field field : fields) {
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
        XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.view.ForumHeaderView", classLoader, "setSearchHint", String.class, XC_MethodReplacement.returnConstant(null));
        //进吧大家都在搜
        XposedHelpers.findAndHookMethod("com.baidu.tieba.homepage.framework.indicator.NestedScrollHeader", classLoader, "setSearchHint", String.class, XC_MethodReplacement.returnConstant(null));
        //首页任务弹窗
        XposedHelpers.findAndHookMethod("com.baidu.tieba.missionCustomDialog.MissionCustomDialogActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                activity.finish();
            }
        });
        //一键签到广告
        XposedHelpers.findAndHookMethod("com.baidu.tieba.signall.SignAllForumAdvertActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                activity.finish();
            }
        });
    }
}