package gm.tieba.tabswitch.hooker.eliminate

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.webkit.WebView
import android.webkit.WebViewClient
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher
import gm.tieba.tabswitch.util.findFirstMethodByExactReturnType
import gm.tieba.tabswitch.util.getAssetFileContent
import gm.tieba.tabswitch.util.getObjectField
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

class Purge : XposedContext(), IHooker, Obfuscated {

    override fun key(): String {
        return "purge"
    }

    override fun matchers(): List<Matcher> {
        return listOf(
            SmaliMatcher("Lcom/baidu/tieba/recapp/lego/model/AdCard;-><init>(Lorg/json/JSONObject;)V"),
            SmaliMatcher("Lcom/baidu/tieba/lego/card/model/BaseCardInfo;-><init>(Lorg/json/JSONObject;)V"),
            StringMatcher("pic_amount"),
            StringMatcher("准备展示精灵动画提示控件"),
            StringMatcher("bottom_bubble_config"),
//            StringMatcher("top_level_navi"),
            StringMatcher("index_tab_info"),
            SmaliMatcher("Lcom/baidu/tbadk/coreExtra/floatCardView/AlaLiveTipView;-><init>(Landroid/content/Context;)V"),
            SmaliMatcher("Lcom/baidu/tbadk/editortools/meme/pan/SpriteMemePan;-><init>(Landroid/content/Context;)V"),
            StringMatcher("h5_pop_ups_config")
        )
    }

    override fun hook() {
        findRule(matchers()) { matcher, clazz, method ->
            when (matcher) {
                // 卡片广告
                "Lcom/baidu/tieba/recapp/lego/model/AdCard;-><init>(Lorg/json/JSONObject;)V", "Lcom/baidu/tieba/lego/card/model/BaseCardInfo;-><init>(Lorg/json/JSONObject;)V" ->
                    XposedBridge.hookAllMethods(
                        findClass(clazz),
                        method,
                        XC_MethodReplacement.returnConstant(null)
                    )

                // 图片广告：必须"recom_ala_info", "app", 可选"goods_info"
                "pic_amount" ->
                    findClass(clazz).declaredMethods.filter { md ->
                        md.parameterTypes.contentToString().contains("JSONObject") && md.name != method
                    }.forEach { md -> hookReplaceMethod(md) {null} }

                // 吧内%s新贴热议中
                "准备展示精灵动画提示控件" ->
                    XposedBridge.hookAllMethods(
                        findClass(clazz),
                        method,
                        XC_MethodReplacement.returnConstant(false)
                    )

                // 底部导航栏活动图标
                "bottom_bubble_config" ->
                    if (method == "invoke") {
                        hookBeforeMethod(clazz, method) { param ->
                            getObjectField(param.thisObject, JSONObject::class.java)?.apply {
                                put("bottom_bubble_config", null)
                            }
                        }
                    }

//                // 首页活动背景
//                "top_level_navi" ->
//                    if (method == "invoke") {
//                        hookBeforeMethod(clazz, method) { param ->
//                            getObjectField(param.thisObject, JSONObject::class.java)?.apply {
//                                put("top_level_navi", null)
//                            }
//                        }
//                    }

                // 首页活动Tab (202), 直播Tab (6)
                "index_tab_info" ->
                    if (method == "invoke") {
                        hookBeforeMethod(clazz, method) { param ->
                            getObjectField(param.thisObject, JSONObject::class.java)?.apply {
                                val indexTabInfo = getJSONArray("index_tab_info")
                                val newIndexTabInfo = JSONArray()
                                if (DeobfuscationHelper.isTbSatisfyVersionRequirement("12.59")) {
                                    for (i in 0 until indexTabInfo.length()) {
                                        val currTab = indexTabInfo.getJSONObject(i)
                                        if (currTab.getString("is_main_tab") == "1" && currTab.getString("tab_type") != "6") {
                                            newIndexTabInfo.put(currTab)
                                        }
                                    }
                                } else {
                                    for (i in 0 until indexTabInfo.length()) {
                                        val currTab = indexTabInfo.getJSONObject(i)
                                        if (currTab.getString("tab_type") != "202" && currTab.getString("tab_type") != "6") {
                                            newIndexTabInfo.put(currTab)
                                        }
                                    }
                                }
                                put("index_tab_info", newIndexTabInfo)
                            }
                        }
                    }

                // 首页左上直播
                "Lcom/baidu/tbadk/coreExtra/floatCardView/AlaLiveTipView;-><init>(Landroid/content/Context;)V" ->
                    hookReplaceMethod(clazz, method, "android.view.ViewGroup") { null }

                // 点我快速配图经验+3
                "Lcom/baidu/tbadk/editortools/meme/pan/SpriteMemePan;-><init>(Landroid/content/Context;)V" ->
                    hookReplaceMethod(clazz, method, "android.content.Context") { null }

                // 各种云控弹窗
                "h5_pop_ups_config" -> if (method == "invoke") {
                    hookBeforeMethod(clazz, method) { param ->
                        getObjectField(param.thisObject, JSONObject::class.java)?.apply {
                            put("h5_pop_ups", null)
                            put("h5_pop_ups_config", null)
                        }
                    }
                }
            }
        }

        // 启动广告
        hookBeforeMethod(
            "com.baidu.adp.framework.MessageManager",
            "findTask", Int::class.javaPrimitiveType
        ) { param ->
            val task = param.args[0] as? Int
            if (task in listOf(2016555, 2921390)) {
                param.setResult(null)
            }
        }

        // 热启动闪屏
        hookReplaceMethod("com.baidu.tbadk.TbSingleton", "isPushLaunch4SplashAd") { true }
        hookReplaceMethod("com.baidu.tbadk.abtest.UbsABTestHelper", "isPushLaunchWithoutSplashAdA") { true }

        // Fix bugs related to isPushLaunch4SplashAd
        hookAfterMethod(
            "com.baidu.tieba.tblauncher.MainTabActivity",
            "onCreate", Bundle::class.java
        ) { _ ->
            XposedHelpers.setStaticBooleanField(
                findClass("com.baidu.tbadk.core.atomData.MainTabActivityConfig"),
                "IS_MAIN_TAB_SPLASH_SHOW",
                false
            )
        }

        // 帖子底部推荐
        @Suppress("DEPRECATION")
        hookBeforeMethod("com.baidu.tieba.pb.pb.main.AbsPbActivity", "onCreate", Bundle::class.java) { param ->
            val activity = param.thisObject as Activity
            val bundle = activity.intent.extras
            val intent = Intent()
            bundle?.let {
                for (key in bundle.keySet()) {
                    when (key) {
                        "key_start_from" -> {   // 为您推荐
                            val startFrom = bundle[key] as? Int
                            if (startFrom in listOf(2, 3)) intent.putExtra(key, 0)
                        }
                        "key_uri" -> {          // 浏览器打开热门推荐
                            val uri = bundle[key] as? Uri
                            uri?.getQueryParameter("tid")?.let { intent.putExtra("thread_id", it) }
                        }
                        else -> {
                            when (val value = bundle[key]) {
                                is Serializable -> intent.putExtra(key, value)
                                is Parcelable -> intent.putExtra(key, value)
                            }
                        }
                    }
                    activity.intent = intent
                }
            }
        }

        // 帖子直播推荐：在 com/baidu/tieba/pb/pb/main/ 中搜索 tbclient/AlaLiveInfo
        hookBeforeMethod(
            "tbclient.AlaLiveInfo\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            XposedHelpers.setObjectField(param.thisObject, "user_info", null)
        }

        // 首页直播推荐卡片：R.layout.card_home_page_ala_live_item_new
        findClass("com.baidu.tieba.homepage.personalize.adapter.HomePageAlaLiveThreadAdapter").declaredMethods.filter { method ->
            method.returnType.toString().endsWith("HomePageAlaLiveThreadViewHolder")
        }.forEach { method -> hookReplaceMethod(method) {null} }

        // 首页推荐
        hookBeforeMethod(
            "tbclient.Personalized.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val threadList = XposedHelpers.getObjectField(param.thisObject, "thread_list") as? MutableList<*>
            threadList?.removeIf { thread ->
                if (XposedHelpers.getObjectField(thread, "forum_info") == null) {
                    return@removeIf true
                }
                if (XposedHelpers.getObjectField(thread, "ala_info") != null) {
                    return@removeIf true
                }
                val worksInfo = XposedHelpers.getObjectField(thread, "works_info")
                worksInfo != null && XposedHelpers.getObjectField(worksInfo, "is_works") as? Int == 1
            }

            // 推荐置顶广告
            XposedHelpers.setObjectField(param.thisObject, "live_answer", null)

            // 圈层热贴
            XposedHelpers.setObjectField(param.thisObject, "hot_card", null)

            // 添加兴趣，为你精准推荐相关内容~
            XposedHelpers.setObjectField(param.thisObject, "interest_class", null)

            // 你可能感兴趣的吧
            XposedHelpers.setObjectField(param.thisObject, "hot_recomforum_top", null)
        }

        // 帖子 AI 聊天
        hookBeforeMethod(
            "tbclient.PbPage.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val postList = XposedHelpers.getObjectField(param.thisObject, "post_list") as? MutableList<*>
            postList?.removeIf { post -> XposedHelpers.getObjectField(post, "aichat_bot_comment_card") != null }
        }

        // 吧页面
        hookBeforeMethod(
            "tbclient.FrsPage.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            // 吧公告
            XposedHelpers.setObjectField(param.thisObject, "star_enter", ArrayList<Any>())

            // 万人直播互动 吧友开黑组队中
            XposedHelpers.setObjectField(param.thisObject, "live_fuse_forum", ArrayList<Any>())

            // AI 聊天
            XposedHelpers.setObjectField(param.thisObject, "ai_chatroom_guide", null)

            // 聊天室
            XposedHelpers.setObjectField(param.thisObject, "frs_bottom", null)

            // 吧友直播
            val frsMainTabList = XposedHelpers.getObjectField(param.thisObject, "frs_main_tab_list") as? MutableList<*>
            frsMainTabList?.removeIf { tab -> XposedHelpers.getObjectField(tab, "tab_type") as Int == 92 }

            // 弹出广告
            XposedHelpers.setObjectField(param.thisObject, "business_promot", null)

            // 顶部背景
            XposedHelpers.setObjectField(param.thisObject, "activityhead", null)
        }

        // 吧友直播
        hookBeforeMethod("tbclient.FrsPage.NavTabInfo\$Builder",
            "build", Boolean::class.javaPrimitiveType) { param ->
            val tabList = XposedHelpers.getObjectField(param.thisObject, "tab") as? MutableList<*>
            tabList?.removeIf { tab -> XposedHelpers.getObjectField(tab, "tab_type") as Int == 92 }
        }

        // 你可能感兴趣的人
        hookBeforeMethod(
            "com.baidu.tieba.feed.list.TemplateAdapter",
            "setList", MutableList::class.java
        ) { param ->
            val feedList = param.args[0] as? MutableList<*>
            feedList?.removeIf { feed ->
                feed?.let {
                    val md = findFirstMethodByExactReturnType(feed.javaClass, String::class.java)
                    val type = XposedHelpers.callMethod(feed, md.name)
                    "sideway_card" == type
                } ?: false
            }
        }

        // 一键签到广告
        hookBeforeMethod("com.baidu.tieba.signall.SignAllForumAdvertActivity",
            "onCreate", Bundle::class.java) { param ->
            val activity = param.thisObject as? Activity
            activity?.finish()
        }

        // 首页推荐右侧悬浮
        findClass("com.baidu.tbadk.widget.RightFloatLayerView").declaredMethods.filter { method ->
            method.parameterTypes.isEmpty() && method.returnType == Boolean::class.javaPrimitiveType
        }.forEach { method -> hookReplaceMethod(method) { false } }

        // 更多板块 (吧友直播，友情吧), 一键签到页面广告
        val jsPurgeScript = getAssetFileContent("Purge.js")
        jsPurgeScript?.let {
            hookBeforeMethod(
                WebViewClient::class.java,
                "onPageStarted", WebView::class.java, String::class.java, Bitmap::class.java
            ) { param ->
                val mWebView = param.args[0] as? WebView
                mWebView?.evaluateJavascript(jsPurgeScript, null)
            }
        }

        // 吧页面
        hookBeforeMethod(
            "tbclient.FrsPage.PageData\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            purgePageData(param.thisObject)
        }
        hookBeforeMethod(
            "tbclient.ThreadList.PageData\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            purgePageData(param.thisObject)
        }

        // 聊天-AI角色
        hookBeforeMethod(
            "com.baidu.tieba.immessagecenter.chatgroup.data.ChatGroupInfo",
            "parse", JSONObject::class.java
        ) { param ->
            val chatGroupInfo = param.args[0] as? JSONObject
            chatGroupInfo?.put("aichat_entrance_info", null)
        }

        // 12.55+
        try {
            // 帖子内广告
            hookReplaceMethod(
                "com.fun.ad.sdk.internal.api.BaseNativeAd2",
                "getNativeInfo"
            ) { null }
        } catch (ignored: ClassNotFoundError) {
        }

        // 帖子内容
        hookBeforeMethod(
            "tbclient.Post\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            // 相关推荐
            XposedHelpers.setObjectField(param.thisObject, "outer_item", null)

            // 点击使用同系列表情
            XposedHelpers.setObjectField(param.thisObject, "sprite_meme_info", null)

            // 小说推荐
            XposedHelpers.setObjectField(param.thisObject, "novel_recom_card", null)

        }

        // 首页样式 AB test
        hookBeforeMethod(
            "com.baidu.tbadk.abtest.UbsABTestDataManager",
            "parseJSONArray",
            JSONArray::class.java
        ) { param ->
            val currentABTestJson = param.args[0] as JSONArray
            val newABTestJson = JSONArray()
            for (i in 0 until currentABTestJson.length()) {
                val currTest = currentABTestJson.getJSONObject(i)
                if (!currTest.getString("sid").startsWith("12_57_5_home_search")) {
                    newABTestJson.put(currTest)
                }
            }
            param.args[0] = newABTestJson
        }
    }

    // 吧页面头条贴(41), 直播贴(69 / is_live_card)
    private fun purgePageData(pageData: Any) {
        val feedList = XposedHelpers.getObjectField(pageData, "feed_list") as? MutableList<*>

        feedList?.removeIf { feedItem ->
            val currFeed = XposedHelpers.getObjectField(feedItem, "feed")

            currFeed?.let { feed ->
                val businessInfo = XposedHelpers.getObjectField(feed, "business_info") as? List<*>

                businessInfo?.any { feedKV ->
                    val currKey = XposedHelpers.getObjectField(feedKV, "key").toString()
                    when (currKey) {
                        "thread_type" -> {
                            val currValue = XposedHelpers.getObjectField(feedKV, "value").toString()
                            currValue in listOf("41", "69")
                        }
                        "is_live_card" -> {
                            val currValue = XposedHelpers.getObjectField(feedKV, "value").toString()
                            currValue == "1"
                        }
                        "game_ext" -> true
                        else -> false
                    }
                } ?: false
            } ?: false
        }
    }
}
