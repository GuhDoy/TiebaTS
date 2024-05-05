package gm.tieba.tabswitch

import android.app.Activity
import android.app.AlertDialog
import android.app.AppComponentFactory
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.content.res.XModuleResources
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import gm.tieba.tabswitch.Constants.strings
import gm.tieba.tabswitch.dao.AcRules
import gm.tieba.tabswitch.dao.AcRules.isRuleFound
import gm.tieba.tabswitch.dao.Adp.initializeAdp
import gm.tieba.tabswitch.dao.Preferences
import gm.tieba.tabswitch.dao.Preferences.getAll
import gm.tieba.tabswitch.dao.Preferences.getBoolean
import gm.tieba.tabswitch.dao.Preferences.putSignature
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.TSPreference
import gm.tieba.tabswitch.hooker.add.HistoryCache
import gm.tieba.tabswitch.hooker.add.Ripple
import gm.tieba.tabswitch.hooker.add.SaveImages
import gm.tieba.tabswitch.hooker.add.SelectClipboard
import gm.tieba.tabswitch.hooker.auto.AgreeNum
import gm.tieba.tabswitch.hooker.auto.AutoSign
import gm.tieba.tabswitch.hooker.auto.FrsTab
import gm.tieba.tabswitch.hooker.auto.MsgCenterTab
import gm.tieba.tabswitch.hooker.auto.NotificationDetect
import gm.tieba.tabswitch.hooker.auto.OpenSign
import gm.tieba.tabswitch.hooker.auto.OriginSrc
import gm.tieba.tabswitch.hooker.auto.TransitionAnimation
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.getTbVersion
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.isTbBetweenVersionRequirement
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.isTbSatisfyVersionRequirement
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.isVersionChanged
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.saveAndRestart
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHooker
import gm.tieba.tabswitch.hooker.eliminate.ContentFilter
import gm.tieba.tabswitch.hooker.eliminate.FoldTopCardView
import gm.tieba.tabswitch.hooker.eliminate.FollowFilter
import gm.tieba.tabswitch.hooker.eliminate.FragmentTab
import gm.tieba.tabswitch.hooker.eliminate.FrsPageFilter
import gm.tieba.tabswitch.hooker.eliminate.PersonalizedFilter
import gm.tieba.tabswitch.hooker.eliminate.Purge
import gm.tieba.tabswitch.hooker.eliminate.PurgeEnter
import gm.tieba.tabswitch.hooker.eliminate.PurgeMy
import gm.tieba.tabswitch.hooker.eliminate.PurgeVideo
import gm.tieba.tabswitch.hooker.eliminate.RedTip
import gm.tieba.tabswitch.hooker.eliminate.RemoveUpdate
import gm.tieba.tabswitch.hooker.eliminate.UserFilter
import gm.tieba.tabswitch.hooker.extra.ForbidGesture
import gm.tieba.tabswitch.hooker.extra.Hide
import gm.tieba.tabswitch.hooker.extra.StackTrace
import gm.tieba.tabswitch.util.fixAlertDialogWidth
import gm.tieba.tabswitch.util.getDialogTheme
import gm.tieba.tabswitch.widget.TbToast
import java.util.Locale
import kotlin.String

class XposedInit : XposedContext(), IXposedHookZygoteInit, IXposedHookLoadPackage {

    private lateinit var mAppComponentFactory: AppComponentFactory

    override fun initZygote(startupParam: StartupParam) {
        sPath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (("com.baidu.tieba" != lpparam.packageName && XposedHelpers.findClassIfExists(
                "com.baidu.tieba.tblauncher.MainTabActivity", lpparam.classLoader
            ) == null) || !lpparam.isFirstApplication
        ) return

        sClassLoader = lpparam.classLoader
        sAssetManager = XModuleResources.createInstance(sPath, null).assets
        mAppComponentFactory = sClassLoader.loadClass("com.baidu.nps.hook.component.NPSComponentFactory")
            .getDeclaredConstructor().newInstance() as AppComponentFactory

        // For some reason certain flutter page will not load in LSPatch unless we manually load the flutter plugin
        hookAfterMethod(
            "com.baidu.tieba.flutter.FlutterPluginManager",
            "invokePlugin", "com.baidu.nps.main.invoke.IInvokeCallback"
        ) { param ->
            try {
                val npsManager = XposedHelpers.callStaticMethod(
                    findClass("com.baidu.nps.main.manager.NPSManager"),
                    "getInstance"
                )
                XposedHelpers.callMethod(
                    npsManager,
                    "loadClazz",
                    "com.baidu.tieba.plugin.flutter",
                    "com.baidu.tieba.flutter.FlutterPluginImpl",
                    findClass("com.baidu.tieba.flutter.IFlutterPlugin"),
                    param.args[0]
                )
            } catch (ignored: Error) {
            }
        }

        hookAfterMethod(
            Instrumentation::class.java,
            "callApplicationOnCreate", Application::class.java
        ) { param ->
            if (param.args[0] !is Application) return@hookAfterMethod

            attachBaseContext((param.args[0] as Application))
            Preferences.init(getContext())
            AcRules.init(getContext())
            DeobfuscationHelper.sCurrentTbVersion = getTbVersion(getContext())

            // Workaround to address an issue with LSPatch (unable to open personal homepage)
            // com.baidu.tieba.flutter.base.view.FlutterPageActivity must be instantiated by com.baidu.nps.hook.component.NPSComponentFactory
            // However, LSPatch incorrectly sets appComponentFactory to null, causing android.app.Instrumentation.getFactory to fall back to AppComponentFactory.DEFAULT
            // (see https://github.com/LSPosed/LSPatch/blob/bbe8d93fb9230f7b04babaf1c4a11642110f55a6/patch-loader/src/main/java/org/lsposed/lspatch/loader/LSPApplication.java#L173)
            if (getContext().applicationInfo.appComponentFactory == null) {
                hookAfterMethod(
                    Instrumentation::class.java,
                    "getFactory", String::class.java
                ) { hookParam ->
                    if (hookParam.args[0].toString() == "com.baidu.tieba") {
                        hookParam.result = mAppComponentFactory
                    }
                }
            }

            val hookers: List<IHooker> = listOf(
                TSPreference(),
                FragmentTab(),
                Purge(),
                PurgeEnter(),
                PurgeMy(),
                RedTip(),
                FollowFilter(),
                PersonalizedFilter(),
                ContentFilter(),
                FrsPageFilter(),
                HistoryCache(),
                Ripple(),
                SaveImages(),
                AutoSign(),
                OpenSign(),
                OriginSrc(),
                ForbidGesture(),
                AgreeNum(),
                FrsTab(),
                Hide(),
                StackTrace(),
                RemoveUpdate(),
                FoldTopCardView(),
                MsgCenterTab(),
                NotificationDetect(),
                PurgeVideo(),
                SelectClipboard(),
                UserFilter(),
                TransitionAnimation()
            )
            val matchers = ArrayList<Obfuscated>(hookers.size + 1)
            matchers.add(TbToast())
            hookers.forEach { hooker ->
                if (hooker is Obfuscated) {
                    matchers.add(hooker as Obfuscated)
                }
            }

            val matchersList = matchers.flatMap { it.matchers() }.toMutableList()

            // Remove matchers that does not satisfy version requirement
            matchersList.removeIf { matcher ->
                matcher.requiredVersion?.let { requiredVersion ->
                    val isVersionSatisfied = isTbSatisfyVersionRequirement(requiredVersion)
                    if (!isVersionSatisfied) {
                        XposedBridge.log(
                            "Skipping rule [%s] due to version mismatch (current version: %s)".format(
                                Locale.CHINA,
                                matcher.toString(),
                                DeobfuscationHelper.sCurrentTbVersion
                            )
                        )
                        true
                    } else {
                        false
                    }
                } ?: false
            }

            if (isVersionChanged(getContext())) {
                if ("com.baidu.tieba" == lpparam.processName) {
                    hookAfterMethod(
                        "com.baidu.tieba.tblauncher.MainTabActivity",
                        "onCreate", Bundle::class.java
                    ) { hookParam ->
                        val activity = hookParam.thisObject as Activity
                        activity.startActivity(Intent(
                            activity,
                            findClass("com.baidu.tieba.LogoActivity")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) })
                    }
                }
                XposedBridge.log("Deobfuscation")

                DeobfuscationHooker(matchersList).hook()
                return@hookAfterMethod
            }

            val lostList = matchersList.map { it.toString() }.filter { !isRuleFound(it) }

            if (lostList.isNotEmpty()) {
                hookBeforeMethod(
                    "com.baidu.tieba.tblauncher.MainTabActivity",
                    "onCreate", Bundle::class.java
                ) { hookParam ->
                    val activity = hookParam.thisObject as Activity
                    val messages = ArrayList<String?>().apply {
                        add(
                            if (isTbBetweenVersionRequirement(BuildConfig.MIN_VERSION, BuildConfig.TARGET_VERSION)) {
                                strings["exception_rules_incomplete"]
                            } else {
                                strings["version_mismatch"]?.format(Locale.CHINA, BuildConfig.MIN_VERSION, BuildConfig.TARGET_VERSION)
                            }
                        )
                        add("贴吧版本：%s, 模块版本：%d".format(Locale.CHINA, DeobfuscationHelper.sCurrentTbVersion, BuildConfig.VERSION_CODE))
                        add("%d rule(s) lost: %s".format(Locale.CHINA, lostList.size, lostList))
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
                        .setNeutralButton("更新模块") { _, _ ->
                            activity.startActivity(Intent().apply {
                                setAction("android.intent.action.VIEW")
                                if (isModuleBetaVersion) {
                                    setData(Uri.parse(strings["ci_uri"]))
                                } else {
                                    setData(Uri.parse(strings["release_uri"]))
                                }
                            })
                        }
                        .setNegativeButton(activity.getString(android.R.string.cancel), null)
                        .setPositiveButton(activity.getString(android.R.string.ok)) { _, _ ->
                            putSignature(0)
                            saveAndRestart(activity, "unknown", null)
                        }.create()

                    alert.show()
                    fixAlertDialogWidth(alert)
                }
                return@hookAfterMethod
            }

            initializeAdp()
            if (getBoolean("hide_native")) {
                try {
                    System.loadLibrary("hide")
                } catch (e: UnsatisfiedLinkError) {
                    XposedBridge.log(e)
                }
            }

            val activeHookerKeys = getAll().entries
                .filter { it.value != false }
                .map { it.key }
                .toMutableSet()
                .apply {
                    add("ts_pref")
                    add("fragment_tab")
                }

            hookers.forEach { hooker ->
                try {
                    if (activeHookerKeys.contains(hooker.key())) {
                        hooker.hook()
                    }
                } catch (tr: Throwable) {
                    XposedBridge.log(tr)
                    exceptions[hooker.key()] = tr
                }
            }
        }
    }
}
