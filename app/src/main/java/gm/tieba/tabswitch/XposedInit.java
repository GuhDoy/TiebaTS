package gm.tieba.tabswitch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.res.XModuleResources;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Adp;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.TSPreference;
import gm.tieba.tabswitch.hooker.add.HistoryCache;
import gm.tieba.tabswitch.hooker.add.Ripple;
import gm.tieba.tabswitch.hooker.add.SaveImages;
import gm.tieba.tabswitch.hooker.add.SelectClipboard;
import gm.tieba.tabswitch.hooker.auto.AgreeNum;
import gm.tieba.tabswitch.hooker.auto.AutoSign;
import gm.tieba.tabswitch.hooker.auto.FrsTab;
import gm.tieba.tabswitch.hooker.auto.MsgCenterTab;
import gm.tieba.tabswitch.hooker.auto.NotificationDetect;
import gm.tieba.tabswitch.hooker.auto.OpenSign;
import gm.tieba.tabswitch.hooker.auto.OriginSrc;
import gm.tieba.tabswitch.hooker.auto.TransitionAnimation;
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper;
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHooker;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.eliminate.ContentFilter;
import gm.tieba.tabswitch.hooker.eliminate.FoldTopCardView;
import gm.tieba.tabswitch.hooker.eliminate.FollowFilter;
import gm.tieba.tabswitch.hooker.eliminate.FragmentTab;
import gm.tieba.tabswitch.hooker.eliminate.FrsPageFilter;
import gm.tieba.tabswitch.hooker.eliminate.PersonalizedFilter;
import gm.tieba.tabswitch.hooker.eliminate.Purge;
import gm.tieba.tabswitch.hooker.eliminate.PurgeEnter;
import gm.tieba.tabswitch.hooker.eliminate.PurgeMy;
import gm.tieba.tabswitch.hooker.eliminate.PurgeVideo;
import gm.tieba.tabswitch.hooker.eliminate.RedTip;
import gm.tieba.tabswitch.hooker.eliminate.RemoveUpdate;
import gm.tieba.tabswitch.hooker.eliminate.UserFilter;
import gm.tieba.tabswitch.hooker.extra.ForbidGesture;
import gm.tieba.tabswitch.hooker.extra.Hide;
import gm.tieba.tabswitch.hooker.extra.StackTrace;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.widget.TbToast;

public class XposedInit extends XposedContext implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    @Override
    public void initZygote(final IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        sPath = startupParam.modulePath;
    }

    private AppComponentFactory mAppComponentFactory = null;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ((!"com.baidu.tieba".equals(lpparam.packageName) && XposedHelpers.findClassIfExists(
                "com.baidu.tieba.tblauncher.MainTabActivity", lpparam.classLoader) == null) || !lpparam.isFirstApplication) return;
        sClassLoader = lpparam.classLoader;
        sAssetManager = XModuleResources.createInstance(sPath, null).getAssets();
        mAppComponentFactory = (AppComponentFactory) sClassLoader.loadClass("com.baidu.nps.hook.component.NPSComponentFactory").newInstance();

        // For some reason certain flutter page will not load in LSPatch unless we manually load the flutter plugin
        XposedHelpers.findAndHookMethod(
                "com.baidu.tieba.flutter.FlutterPluginManager",
                sClassLoader,
                "invokePlugin",
                "com.baidu.nps.main.invoke.IInvokeCallback",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object npsManager = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.baidu.nps.main.manager.NPSManager", sClassLoader), "getInstance");
                            XposedHelpers.callMethod(npsManager, "loadClazz",
                                    "com.baidu.tieba.plugin.flutter",
                                    "com.baidu.tieba.flutter.FlutterPluginImpl",
                                    XposedHelpers.findClass("com.baidu.tieba.flutter.IFlutterPlugin", sClassLoader),
                                    param.args[0]
                            );
                        } catch (Error ignored) {}
                    }
                }
        );

        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                if (!(param.args[0] instanceof Application)) return;
                attachBaseContext((Application) param.args[0]);
                Preferences.init(getContext());
                AcRules.init(getContext());
                String currTbVersion = DeobfuscationHelper.getTbVersion(getContext());

                // Workaround to address an issue with LSPatch (unable to open personal homepage)
                // com.baidu.tieba.flutter.base.view.FlutterPageActivity must be instantiated by com.baidu.nps.hook.component.NPSComponentFactory
                // However, LSPatch incorrectly sets appComponentFactory to null, causing android.app.Instrumentation.getFactory to fall back to AppComponentFactory.DEFAULT
                // (see https://github.com/LSPosed/LSPatch/blob/bbe8d93fb9230f7b04babaf1c4a11642110f55a6/patch-loader/src/main/java/org/lsposed/lspatch/loader/LSPApplication.java#L173)
                if (getContext().getApplicationInfo().appComponentFactory == null) {
                    XposedBridge.log("Applying AppComponentFactory workaround");
                    XposedHelpers.findAndHookMethod(
                            Instrumentation.class,
                            "getFactory",
                            String.class,
                            new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (param.args[0].toString().equals("com.baidu.tieba")) {
                                        param.setResult(mAppComponentFactory);
                                    }
                                }
                            });
                }

                final var hookers = List.of(
                        new TSPreference(),
                        new FragmentTab(),
                        new Purge(),
                        new PurgeEnter(),
                        new PurgeMy(),
                        new RedTip(),
                        new FollowFilter(),
                        new PersonalizedFilter(),
                        new ContentFilter(),
                        new FrsPageFilter(),
                        new HistoryCache(),
                        new Ripple(),
                        new SaveImages(),
                        new AutoSign(),
                        new OpenSign(),
                        new OriginSrc(),
                        new ForbidGesture(),
                        new AgreeNum(),
                        new FrsTab(),
                        new Hide(),
                        new StackTrace(),
                        new RemoveUpdate(),
                        new FoldTopCardView(),
                        new MsgCenterTab(),
                        new NotificationDetect(),
                        new PurgeVideo(),
                        new SelectClipboard(),
                        new UserFilter(),
                        new TransitionAnimation()
                );
                final var matchers = new ArrayList<Obfuscated>(hookers.size() + 2);
                matchers.add(new TbToast());
                for (final var hooker : hookers) {
                    if (hooker instanceof Obfuscated) {
                        matchers.add((Obfuscated) hooker);
                    }
                }

                List<Matcher> matchersList = matchers.stream()
                        .map(Obfuscated::matchers)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

                // Remove matchers that does not satisfy version requirement
                matchersList.removeIf(
                        o -> {
                            if (o.getRequiredVersion() != null) {
                                boolean isVersionSatisfied = DeobfuscationHelper.isTbSatisfyVersionRequirement(
                                        o.getRequiredVersion(),
                                        currTbVersion
                                );
                                if (!isVersionSatisfied) {
                                    XposedBridge.log(
                                            String.format(
                                                    "Skipping rule [%s] due to version mismatch (current version: %s)",
                                                    o.toString(),
                                                    currTbVersion
                                            )
                                    );
                                }
                                return !isVersionSatisfied;
                            }
                            return false;
                        }
                );

                if (DeobfuscationHelper.isVersionChanged(getContext())) {
                    if ("com.baidu.tieba".equals(lpparam.processName)) {
                        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                                final var activity = (Activity) param.thisObject;
                                final var intent = new Intent(activity, XposedHelpers.findClass("com.baidu.tieba.LogoActivity", sClassLoader));
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                activity.startActivity(intent);
                            }
                        });
                    }
                    XposedBridge.log("Deobfuscation");

                    new DeobfuscationHooker(matchersList).hook();
                    return;
                }

                final var lostList = matchersList.stream()
                        .map(Matcher::toString)
                        .filter(matcher -> !AcRules.isRuleFound(matcher))
                        .collect(Collectors.toList());
                if (!lostList.isEmpty()) {
                    XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                            final var activity = (Activity) param.thisObject;
                            final var messages = new ArrayList<String>();

                            if (DeobfuscationHelper.isTbSatisfyVersionRequirement(BuildConfig.MIN_VERSION, currTbVersion)
                                && (!DeobfuscationHelper.isTbSatisfyVersionRequirement(BuildConfig.TARGET_VERSION, currTbVersion) || currTbVersion.equals(BuildConfig.TARGET_VERSION))) {
                                messages.add(Constants.getStrings().get("exception_rules_incomplete"));
                            } else {
                                messages.add(String.format(Locale.CHINA,
                                        Constants.getStrings().get("version_mismatch"),
                                        BuildConfig.MIN_VERSION, BuildConfig.TARGET_VERSION));
                            }

                            messages.add(String.format(Locale.CHINA, "贴吧版本：%s, 模块版本：%d",
                                    currTbVersion, BuildConfig.VERSION_CODE));
                            messages.add(String.format(Locale.CHINA, "%d rule(s) lost: %s", lostList.size(), lostList));
                            final var message = TextUtils.join("\n", messages);
                            XposedBridge.log(message);
                            AlertDialog alert = new AlertDialog.Builder(activity, DisplayUtils.isLightMode(getContext()) ?
                                    android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                    .setTitle("规则异常").setMessage(message).setCancelable(false)
                                    .setNeutralButton("更新模块", (dialogInterface, i) -> {
                                        final Intent intent = new Intent();
                                        intent.setAction("android.intent.action.VIEW");
                                        intent.setData(Uri.parse("https://github.com/GuhDoy/TiebaTS/releases"));
                                        activity.startActivity(intent);
                                    })
                                    .setNegativeButton(activity.getString(android.R.string.cancel), null)
                                    .setPositiveButton(activity.getString(android.R.string.ok), (dialogInterface, i) -> {
                                        Preferences.putSignature(0);
                                        DeobfuscationHelper.saveAndRestart(activity, "unknown", null);
                                    }).create();
                            alert.show();
                            DisplayUtils.fixAlertDialogWidth(alert);
                        }
                    });
                    return;
                }

                new Adp();
                if (Preferences.getBoolean("hide_native")) {
                    try {
                        System.loadLibrary("hide");
                    } catch (final UnsatisfiedLinkError e) {
                        XposedBridge.log(e);
                    }
                }
                final var activeHookerKeys = Preferences.getAll().entrySet().stream()
                        .filter(entry -> Boolean.FALSE != entry.getValue())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                activeHookerKeys.add("ts_pref");
                activeHookerKeys.add("fragment_tab");
                for (final var hooker : hookers) {
                    try {
                        if (activeHookerKeys.contains(hooker.key())) {
                            hooker.hook();
                        }
                    } catch (final Throwable tr) {
                        XposedBridge.log(tr);
                        sExceptions.put(hooker.key(), tr);
                    }
                }
            }
        });
    }
}
