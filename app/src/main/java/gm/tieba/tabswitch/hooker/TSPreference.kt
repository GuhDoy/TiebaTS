package gm.tieba.tabswitch.hooker

import android.R
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.BuildConfig
import gm.tieba.tabswitch.Constants.strings
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.dao.Preferences.commit
import gm.tieba.tabswitch.dao.Preferences.getBoolean
import gm.tieba.tabswitch.dao.Preferences.getIsAutoSignEnabled
import gm.tieba.tabswitch.dao.Preferences.getIsEULAAccepted
import gm.tieba.tabswitch.dao.Preferences.getIsPurgeEnabled
import gm.tieba.tabswitch.dao.Preferences.getTransitionAnimationEnabled
import gm.tieba.tabswitch.dao.Preferences.putAutoSignEnabled
import gm.tieba.tabswitch.dao.Preferences.putEULAAccepted
import gm.tieba.tabswitch.dao.Preferences.putPurgeEnabled
import gm.tieba.tabswitch.hooker.TSPreferenceHelper.PreferenceLayout
import gm.tieba.tabswitch.hooker.TSPreferenceHelper.SwitchButtonHolder
import gm.tieba.tabswitch.hooker.TSPreferenceHelper.createButton
import gm.tieba.tabswitch.hooker.TSPreferenceHelper.createTextView
import gm.tieba.tabswitch.hooker.TSPreferenceHelper.randomToast
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.getTbVersion
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.isTbSatisfyVersionRequirement
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher
import gm.tieba.tabswitch.hooker.extra.TraceChecker
import gm.tieba.tabswitch.util.fixAlertDialogWidth
import gm.tieba.tabswitch.util.getDialogTheme
import gm.tieba.tabswitch.util.isLightMode
import gm.tieba.tabswitch.util.restart
import gm.tieba.tabswitch.widget.NavigationBar
import gm.tieba.tabswitch.widget.TbToast
import gm.tieba.tabswitch.widget.TbToast.Companion.showTbToast
import org.luckypray.dexkit.query.matchers.ClassMatcher
import java.util.Locale

class TSPreference : XposedContext(), IHooker, Obfuscated {
    override fun key(): String {
        return "ts_pref"
    }

    override fun matchers(): List<Matcher> {
        return listOf(
            SmaliMatcher(
                "Lcom/baidu/tbadk/data/MetaData;->getBazhuGradeData()Lcom/baidu/tbadk/coreExtra/data/BazhuGradeData;"
            ).apply {
                classMatcher = ClassMatcher.create().usingStrings("mo/q/wise-bawu-core/privacy-policy")
            }
        )
    }

    @Throws(Throwable::class)
    override fun hook() {
        hookBeforeMethod(
            Dialog::class.java,
            "dismissDialog"
        ) { param ->
            val dialog = param.thisObject as? Dialog
            dialog?.takeIf { it.isShowing }?.window?.currentFocus?.let { view ->
                val imm = view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(view.rootView.windowToken, 0)
            }
        }

        hookAfterMethod(
            "com.baidu.tieba.setting.more.MoreActivity",
            "onCreate", Bundle::class.java
        ) { param ->
            val activity = param.thisObject as Activity
            val contentView = activity.findViewById<ViewGroup>(R.id.content)
            val parent = contentView.getChildAt(0) as RelativeLayout
            val scroll = parent.getChildAt(0) as ScrollView
            val containerView = scroll.getChildAt(0) as LinearLayout
            containerView.addView(createButton(MAIN, null, true) { startRootPreferenceActivity(activity) }, 11)
        }

        findRule(matchers()) { _, clazz, _ ->
            try {
                hookAfterConstructor(clazz, findClass(PROXY_ACTIVITY)) { param ->
                    val activity = param.args[0] as Activity
                    try {
                        val navigationBar = NavigationBar(param.thisObject)
                        val proxyPage = activity.intent.getStringExtra("proxyPage") ?: return@hookAfterConstructor
                        when (proxyPage) {
                            MAIN -> proxyPage(activity, navigationBar, MAIN, createRootPreference(activity))
                            MODIFY_TAB -> proxyPage(activity, navigationBar, MODIFY_TAB, createModifyTabPreference(activity))
                            TRACE -> proxyPage(activity, navigationBar, TRACE, createHidePreference(activity))
                        }

                    } catch (tr: Throwable) {
                        val messages = ArrayList<String?>().apply {
                            add(strings["exception_init_preference"])
                            add(
                                "贴吧版本：%s, 模块版本：%d".format(Locale.CHINA, getTbVersion(getContext()), BuildConfig.VERSION_CODE)
                            )
                            add(Log.getStackTraceString(tr))
                        }

                        val message = TextUtils.join("\n", messages)
                        XposedBridge.log(message)

                        val alert = AlertDialog.Builder(
                            activity,
                            getDialogTheme(getContext())
                        )
                            .setTitle("规则异常")
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(activity.getString(R.string.ok)) { _, _ -> activity.finish() }
                            .create()

                        alert.show()
                        fixAlertDialogWidth(alert)
                    }
                }
            } catch (ignored: NoSuchMethodError) {
            }
        }
    }

    @Throws(Throwable::class)
    private fun proxyPage(activity: Activity, navigationBar: NavigationBar, title: String, preferenceLayout: LinearLayout) {
        navigationBar.apply {
            setTitleText(title)
            setCenterTextTitle("")
            addTextButton("重启") { _ ->
                commit()
                restart(activity)
            }
        }

        val contentView = activity.findViewById<ViewGroup>(R.id.content)
        val parent = contentView.getChildAt(0) as LinearLayout
        val mainScroll = parent.getChildAt(1) as ScrollView
        val containerView = mainScroll.getChildAt(0) as LinearLayout

        containerView.apply {
            removeAllViews()
            addView(preferenceLayout)
        }
    }

    private fun startRootPreferenceActivity(activity: Activity) {
        if (!getIsEULAAccepted()) {
            val stringBuilder = StringBuilder().apply {
                append(strings["EULA"])
                if (isModuleBetaVersion) {
                    append("\n\n").append(strings["dev_tip"])
                }
            }

            val alert = AlertDialog.Builder(
                activity,
                getDialogTheme(getContext())
            )
                .setTitle("使用协议")
                .setMessage(stringBuilder.toString())
                .setNegativeButton(activity.getString(R.string.cancel), null)
                .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
                    putEULAAccepted()
                    startRootPreferenceActivity(activity)
                }
                .create()

            alert.show()
            fixAlertDialogWidth(alert)

        } else {
            activity.startActivity(Intent().apply {
                setClassName(activity, PROXY_ACTIVITY)
                putExtra("proxyPage", MAIN)
            })
        }
    }

    private fun createRootPreference(activity: Activity): LinearLayout {
        val isPurgeEnabled = getIsPurgeEnabled()
        val preferenceLayout = PreferenceLayout(activity)

        preferenceLayout.addView(createTextView(if (isPurgeEnabled) "轻车简从" else "净化界面"))
        preferenceLayout.addView(createButton(MODIFY_TAB, null, true) { _ ->
            activity.startActivity(Intent().apply {
                setClassName(activity, PROXY_ACTIVITY)
                putExtra("proxyPage", MODIFY_TAB)
            })
        })

        if (isPurgeEnabled) {
            preferenceLayout.addView(SwitchButtonHolder(activity, "真正的净化界面", "purge", SwitchButtonHolder.TYPE_SWITCH))
        }

        preferenceLayout.addView(SwitchButtonHolder(activity, "净化进吧", "purge_enter", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "净化我的", "purge_my", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "默认折叠置顶帖", "fold_top_card_view", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "只推荐已关注的吧", "follow_filter", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "屏蔽首页视频贴", "purge_video", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "过滤首页推荐", "personalized_filter", SwitchButtonHolder.TYPE_DIALOG))
        preferenceLayout.addView(SwitchButtonHolder(activity, "过滤吧页面", "frs_page_filter", SwitchButtonHolder.TYPE_DIALOG))
        preferenceLayout.addView(SwitchButtonHolder(activity, "过滤帖子回复", "content_filter", SwitchButtonHolder.TYPE_DIALOG))
        preferenceLayout.addView(SwitchButtonHolder(activity, "过滤用户", "user_filter", SwitchButtonHolder.TYPE_DIALOG))

        preferenceLayout.addView(createTextView(if (isPurgeEnabled) "别出新意" else "增加功能"))
        preferenceLayout.addView(SwitchButtonHolder(activity, "浏览历史增加搜索", "history_cache", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "楼层增加点按效果", "ripple", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "长按下载保存全部图片", "save_images", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "弹窗自由复制", "select_clipboard", SwitchButtonHolder.TYPE_SWITCH))

        preferenceLayout.addView(createTextView(if (isPurgeEnabled) "垂手可得" else "自动化"))

        val autoSign = SwitchButtonHolder(activity, "自动签到", "auto_sign", SwitchButtonHolder.TYPE_SWITCH)
        autoSign.setOnButtonClickListener { _ ->
            if (!getIsAutoSignEnabled()) {
                val alert = AlertDialog.Builder(
                    activity,
                    getDialogTheme(getContext())
                )
                    .setTitle("提示")
                    .setMessage("这是一个需要网络请求并且有封号风险的功能，您需要自行承担使用此功能的风险，请谨慎使用！")
                    .setNegativeButton(activity.getString(R.string.cancel), null)
                    .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
                        putAutoSignEnabled()
                        autoSign.bdSwitch.turnOn()
                    }
                    .create()
                alert.show()
                fixAlertDialogWidth(alert)

            } else {
                autoSign.bdSwitch.changeState()
            }
        }
        preferenceLayout.addView(autoSign)

        preferenceLayout.addView(SwitchButtonHolder(activity, "自动打开一键签到", "open_sign", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "吧页面起始页面改为最新", "frs_tab", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "消息页面起始页面改为通知", "msg_center_tab", SwitchButtonHolder.TYPE_SWITCH))

        val originSrcOnlyWifiButton = SwitchButtonHolder(activity, "自动查看原图仅WiFi下生效", "origin_src_only_wifi", SwitchButtonHolder.TYPE_SWITCH)
        val originSrcButton = SwitchButtonHolder(activity, "自动查看原图", "origin_src", SwitchButtonHolder.TYPE_SWITCH)
        originSrcButton.setOnButtonClickListener { _ ->
            originSrcButton.bdSwitch.changeState()
            originSrcOnlyWifiButton.switchButton.visibility = if (getBoolean("origin_src")) View.VISIBLE else View.GONE
        }
        originSrcOnlyWifiButton.switchButton.visibility = if (getBoolean("origin_src")) View.VISIBLE else View.GONE

        preferenceLayout.addView(originSrcButton)
        preferenceLayout.addView(originSrcOnlyWifiButton)

        preferenceLayout.addView(createTextView(if (isPurgeEnabled) "奇怪怪" else "其它"))
        preferenceLayout.addView(SwitchButtonHolder(activity, "隐藏小红点", "red_tip", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "禁用更新提示", "remove_update", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "禁用帖子手势", "forbid_gesture", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "用赞踩差数代替赞数", "agree_num", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "禁止检测通知开启状态", "notification_detect", SwitchButtonHolder.TYPE_SWITCH))

        preferenceLayout.addView(createButton(TRACE, "希望有一天不再需要贴吧TS", true) { _ ->
            activity.startActivity(Intent().apply {
                setClassName(activity, PROXY_ACTIVITY)
                putExtra("proxyPage", TRACE)
            })
        })

        preferenceLayout.addView(createTextView(if (isPurgeEnabled) "关于就是关于" else "关于"))
        preferenceLayout.addView(createButton("作者", "GM", true) { _ ->
            sCount++
            if (sCount % 3 == 0) {
                showTbToast(randomToast(), TbToast.LENGTH_SHORT)
            }
            if (!isPurgeEnabled && sCount >= 10) {
                putPurgeEnabled()
                activity.recreate()
            }
        })

        preferenceLayout.addView(createButton("源代码", "想要小星星", true) { _ ->
            activity.startActivity(Intent().apply {
                setAction("android.intent.action.VIEW")
                setData(Uri.parse("https://github.com/GuhDoy/TiebaTS"))
            })
        })

        preferenceLayout.addView(createButton("TG群", "及时获取更新", true) { _ ->
            activity.startActivity(Intent().apply {
                setAction("android.intent.action.VIEW")
                setData(Uri.parse("https://t.me/TabSwitch"))
            })
        })

        preferenceLayout.addView(
            createButton("版本", "%s".format(Locale.CHINA, BuildConfig.VERSION_NAME), true) { _ ->
                activity.startActivity(Intent().apply {
                    setAction("android.intent.action.VIEW")
                    if (isModuleBetaVersion) {
                        setData(Uri.parse(strings["ci_uri"]))
                    } else {
                        setData(Uri.parse(strings["release_uri"]))
                    }
                })
            })
        return preferenceLayout
    }

    private fun createModifyTabPreference(activity: Activity): LinearLayout {
        val preferenceLayout = PreferenceLayout(activity)
        preferenceLayout.addView(createTextView("主页导航栏"))
        preferenceLayout.addView(SwitchButtonHolder(activity, "隐藏首页", "home_recommend", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "隐藏进吧", "enter_forum", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "隐藏发帖", "write_thread", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "隐藏消息", "im_message", SwitchButtonHolder.TYPE_SWITCH))

        preferenceLayout.addView(createTextView("其他"))
        val transitionAnimation = SwitchButtonHolder(activity, "修复过渡动画", "transition_animation", SwitchButtonHolder.TYPE_SWITCH)

        val shouldEnableTransitionAnimationFix = Build.VERSION.SDK_INT >= 34 && isTbSatisfyVersionRequirement("12.58.2.1")
        if (!shouldEnableTransitionAnimationFix && getTransitionAnimationEnabled()) {
            transitionAnimation.bdSwitch.turnOff()
        }

        transitionAnimation.setOnButtonClickListener { _ ->
            if (!shouldEnableTransitionAnimationFix) {
                showTbToast("当前贴吧版本不支持此功能", TbToast.LENGTH_SHORT)
            } else {
                transitionAnimation.bdSwitch.changeState()
            }
        }
        preferenceLayout.addView(transitionAnimation)

        return preferenceLayout
    }

    private fun createHidePreference(activity: Activity): LinearLayout {
        val isPurgeEnabled = getIsPurgeEnabled()
        val preferenceLayout = PreferenceLayout(activity)
        if (isPurgeEnabled || BuildConfig.DEBUG) {
            preferenceLayout.addView(createTextView("隐藏设置"))
            preferenceLayout.addView(SwitchButtonHolder(activity, if (isPurgeEnabled) "藏起尾巴" else "隐藏模块", "hide", SwitchButtonHolder.TYPE_SWITCH))
            preferenceLayout.addView(SwitchButtonHolder(activity, if (isPurgeEnabled) "藏起尾巴（原生）" else "隐藏模块（原生）", "hide_native", SwitchButtonHolder.TYPE_SWITCH))
        }

        preferenceLayout.addView(createTextView("检测设置"))
        preferenceLayout.addView(SwitchButtonHolder(activity, "检测 Xposed", "check_xposed", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "检测模块", "check_module", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(SwitchButtonHolder(activity, "检测堆栈（重启才能真正生效）", "check_stack_trace", SwitchButtonHolder.TYPE_SWITCH))
        preferenceLayout.addView(
            createButton(
                if (isPurgeEnabled) "捏捏尾巴" else "检测模块",
                Process.myPid().toString(),
                true
            ) { _ -> TraceChecker(preferenceLayout).checkAll() })
        TraceChecker.sChildCount = preferenceLayout.childCount
        return preferenceLayout
    }

    companion object {
        const val MAIN: String = "贴吧TS设置"
        const val MODIFY_TAB: String = "修改页面"
        const val TRACE: String = "痕迹"
        private const val PROXY_ACTIVITY = "com.baidu.tieba.setting.im.more.SecretSettingActivity"
        private var sCount = 0
    }
}
