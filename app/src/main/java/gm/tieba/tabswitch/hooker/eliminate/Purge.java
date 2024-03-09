package gm.tieba.tabswitch.hooker.eliminate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

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
import gm.tieba.tabswitch.util.FileUtils;
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
                new StringMatcher("key_frs_dialog_ad_last_show_time"),
                new StringMatcher("准备展示精灵动画提示控件"),
                new StringMatcher("TbChannelJsInterfaceNew"),
                new StringMatcher("bottom_bubble_config"),
                new StringMatcher("top_level_navi"),
                new StringMatcher("index_tab_info"),
                new SmaliMatcher("Lcom/baidu/tbadk/coreExtra/floatCardView/AlaLiveTipView;-><init>(Landroid/content/Context;)V"),
                new SmaliMatcher("Lcom/baidu/tbadk/editortools/meme/pan/SpriteMemePan;-><init>(Landroid/content/Context;)V")
        );
    }

    @Override
    public void hook() throws Throwable {
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            switch (matcher) {
                case "Lcom/baidu/tieba/recapp/lego/model/AdCard;-><init>(Lorg/json/JSONObject;)V":
                case "Lcom/baidu/tieba/lego/card/model/BaseCardInfo;-><init>(Lorg/json/JSONObject;)V": // 卡片广告
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
                case "准备展示精灵动画提示控件": // 吧内%s新贴热议中
                    XposedBridge.hookAllMethods(XposedHelpers.findClass(clazz, sClassLoader), method, XC_MethodReplacement.returnConstant(false));
                    break;
                case "TbChannelJsInterfaceNew":  // 吧友直播
                    if (method.equals("getInitData")) {
                        XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                JSONObject resultJson = new JSONObject((String) param.getResult());
                                resultJson.getJSONObject("baseData").put("clientVersion", "undefined");
                                param.setResult(resultJson.toString());
                            }
                        });
                    }
                    break;
                case "bottom_bubble_config":    // 底部导航栏活动图标
                    if (method.equals("invoke")) {
                        XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                JSONObject syncData = (JSONObject) ReflectUtils.getObjectField(param.thisObject, JSONObject.class);
                                syncData.put("bottom_bubble_config", null);
                            }
                        });
                    }
                    break;
                case "top_level_navi":  // 首页活动背景
                    if (method.equals("invoke")) {
                        XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                JSONObject syncData = (JSONObject) ReflectUtils.getObjectField(param.thisObject, JSONObject.class);
                                syncData.put("top_level_navi", null);
                            }
                        });
                    }
                    break;
                case "index_tab_info":  // 首页活动Tab
                    if (method.equals("invoke")) {
                        XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                JSONObject syncData = (JSONObject) ReflectUtils.getObjectField(param.thisObject, JSONObject.class);
                                JSONArray indexTabInfo = syncData.getJSONArray("index_tab_info");
                                JSONArray newIndexTabInfo = new JSONArray();
                                for (int i = 0; i < indexTabInfo.length(); i++) {
                                    JSONObject currTab = indexTabInfo.getJSONObject(i);
                                    if (!currTab.getString("tab_type").equals("202")) {
                                        newIndexTabInfo.put(currTab);
                                    }
                                }
                                syncData.put("index_tab_info", newIndexTabInfo);
                            }
                        });
                    }
                    break;
                case "Lcom/baidu/tbadk/coreExtra/floatCardView/AlaLiveTipView;-><init>(Landroid/content/Context;)V":    // 首页左上直播
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, "android.view.ViewGroup",
                            XC_MethodReplacement.returnConstant(null));
                    break;
                case "Lcom/baidu/tbadk/editortools/meme/pan/SpriteMemePan;-><init>(Landroid/content/Context;)V":    // 点我快速配图经验+3
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, "android.content.Context",
                            XC_MethodReplacement.returnConstant(null));
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
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.TbSingleton", sClassLoader, "isPushLaunch4SplashAd", XC_MethodReplacement.returnConstant(true));
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.abtest.UbsABTestHelper", sClassLoader, "isPushLaunchWithoutSplashAdA", XC_MethodReplacement.returnConstant(true));
        // Fix bugs related to isPushLaunch4SplashAd
        XposedHelpers.findAndHookMethod(
                "com.baidu.tieba.tblauncher.MainTabActivity",
                sClassLoader,
                "onCreate",
                Bundle.class,
                new XC_MethodHook(-1) {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        XposedHelpers.setStaticBooleanField(XposedHelpers.findClass("com.baidu.tbadk.core.atomData.MainTabActivityConfig", sClassLoader), "IS_MAIN_TAB_SPLASH_SHOW", false);
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

                    if (XposedHelpers.getObjectField(o, "ala_info") != null) {
                        return true;
                    }

                    final Object worksInfo = XposedHelpers.getObjectField(o, "works_info");
                    return worksInfo != null && (Integer) XposedHelpers.getObjectField(worksInfo, "is_works") == 1;
                });

                // 推荐置顶广告
                XposedHelpers.setObjectField(param.thisObject, "live_answer", null);

                // 圈层热贴
                XposedHelpers.setObjectField(param.thisObject, "hot_card", null);
            }
        });
        // 帖子 AI 聊天
        XposedHelpers.findAndHookMethod("tbclient.PbPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var postList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "post_list");
                if (postList == null) return;

                postList.removeIf(o -> XposedHelpers.getObjectField(o, "aichat_bot_comment_card") != null);
            }
        });
        // 吧页面
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                // 吧公告
                XposedHelpers.setObjectField(param.thisObject, "star_enter", new ArrayList<>());

                // thread_list is deprecated
//                final List<?> threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
//                if (threadList != null) {
//                    // 吧页面头条贴(41), 直播贴(69)
//                    threadList.removeIf(o -> {
//                        var threadType = (Integer) XposedHelpers.getObjectField(o, "thread_type");
//                        return threadType == 41 || threadType == 69;
//                    });
//                }

                // 万人直播互动 吧友开黑组队中
                XposedHelpers.setObjectField(param.thisObject, "live_fuse_forum", new ArrayList<>());

                // AI 聊天
                XposedHelpers.setObjectField(param.thisObject, "ai_chatroom_guide", null);

                // 聊天室
                XposedHelpers.setObjectField(param.thisObject, "frs_bottom", null);

                // 吧友直播
                final List<?> frsMainTabList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "frs_main_tab_list");
                if (frsMainTabList != null) {
                    frsMainTabList.removeIf(o -> (Integer) XposedHelpers.getObjectField(o, "tab_type") == 92);
                }

                // 弹出广告
                XposedHelpers.setObjectField(param.thisObject, "business_promot", null);

                // 顶部背景
                XposedHelpers.setObjectField(param.thisObject, "activityhead", null);
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
        // 更多板块 (吧友直播，友情吧)
        final String jsPurgeFrsBottom = FileUtils.getAssetFileContent("PurgeFrsBottom.js");
        if (jsPurgeFrsBottom != null) {
            XposedHelpers.findAndHookMethod(WebViewClient.class, "onPageStarted", WebView.class, String.class, Bitmap.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    WebView mWebView = (WebView) param.args[0];
                    mWebView.evaluateJavascript(jsPurgeFrsBottom, null);
                }
            });
        }
        // 吧页面头条贴(41), 直播贴(69 / is_live_card)
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.PageData$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                List<?> feedList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "feed_list");
                if (feedList != null) {
                    feedList.removeIf(
                            o -> {
                                Object currFeed = XposedHelpers.getObjectField(o, "feed");
                                if (currFeed != null) {
                                    List<?> businessInfo = (List<?>) XposedHelpers.getObjectField(currFeed, "business_info");
                                    for (var feedKV : businessInfo) {
                                        String currentKey = XposedHelpers.getObjectField(feedKV, "key").toString();
                                        if (currentKey.equals("thread_type")) {
                                            var currValue = XposedHelpers.getObjectField(feedKV, "value").toString();
                                            if (currValue.equals("41") || currValue.equals("69")) {
                                                return true;
                                            }
                                        } else if (currentKey.equals("is_live_card")) {
                                            var currValue = XposedHelpers.getObjectField(feedKV, "value").toString();
                                            if (currValue.equals("1")) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                                return false;
                            }
                    );
                }
            }
        });
        // 聊天-AI角色
        XposedHelpers.findAndHookMethod("com.baidu.tieba.immessagecenter.chatgroup.data.ChatGroupInfo", sClassLoader, "parse", JSONObject.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                JSONObject chatGroupInfo = (JSONObject) param.args[0];
                chatGroupInfo.put("aichat_entrance_info", null);
            }
        });

        // 12.55+
        try {
            // 帖子内广告
            XposedHelpers.findAndHookMethod("com.fun.ad.sdk.internal.api.BaseNativeAd2", sClassLoader, "getNativeInfo", XC_MethodReplacement.returnConstant(null));
        } catch (final XposedHelpers.ClassNotFoundError ignored) {}

        XposedHelpers.findAndHookMethod("tbclient.Post$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                // 相关推荐
                XposedHelpers.setObjectField(param.thisObject, "outer_item", null);

                // 点击使用同系列表情
                XposedHelpers.setObjectField(param.thisObject, "sprite_meme_info", null);
            }
        });

        // 吧友都在看
        XposedBridge.hookMethod(
                ReflectUtils.findFirstMethodByExactType("com.baidu.tbadk.coreExtra.view.ImagePagerAdapter", ArrayList.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                        ArrayList<String> mList = (ArrayList<String>) param.args[0];
                        mList.removeIf(o -> o.startsWith("####mLiveRoomPageProvider"));
                    }
                });
    }
}
