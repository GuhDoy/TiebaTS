package gm.tieba.tabswitch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
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
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.TSPreference;
import gm.tieba.tabswitch.hooker.add.CreateView;
import gm.tieba.tabswitch.hooker.add.HistoryCache;
import gm.tieba.tabswitch.hooker.add.MyAttention;
import gm.tieba.tabswitch.hooker.add.NewSub;
import gm.tieba.tabswitch.hooker.add.Ripple;
import gm.tieba.tabswitch.hooker.add.SaveImages;
import gm.tieba.tabswitch.hooker.add.ThreadStore;
import gm.tieba.tabswitch.hooker.auto.AgreeNum;
import gm.tieba.tabswitch.hooker.auto.AutoSign;
import gm.tieba.tabswitch.hooker.auto.EyeshieldMode;
import gm.tieba.tabswitch.hooker.auto.FrsTab;
import gm.tieba.tabswitch.hooker.auto.OpenSign;
import gm.tieba.tabswitch.hooker.auto.OriginSrc;
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper;
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHook;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.eliminate.ContentFilter;
import gm.tieba.tabswitch.hooker.eliminate.FollowFilter;
import gm.tieba.tabswitch.hooker.eliminate.FragmentTab;
import gm.tieba.tabswitch.hooker.eliminate.FrsPageFilter;
import gm.tieba.tabswitch.hooker.eliminate.PersonalizedFilter;
import gm.tieba.tabswitch.hooker.eliminate.Purge;
import gm.tieba.tabswitch.hooker.eliminate.RedTip;
import gm.tieba.tabswitch.hooker.eliminate.SwitchManager;
import gm.tieba.tabswitch.hooker.extra.ForbidGesture;
import gm.tieba.tabswitch.hooker.extra.Hide;
import gm.tieba.tabswitch.hooker.extra.RedirectImage;
import gm.tieba.tabswitch.hooker.extra.StackTrace;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbToast;

public class XposedInit extends XposedContext implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        sPath = startupParam.modulePath;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.baidu.tieba".equals(lpparam.packageName) && XposedHelpers.findClassIfExists(
                "com.baidu.tieba.tblauncher.MainTabActivity", lpparam.classLoader) == null) return;
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!(param.args[0] instanceof Application)) return;
                attachBaseContext((Application) param.args[0]);
                sClassLoader = lpparam.classLoader;
                Preferences.init(getContext());
                AcRules.init(getContext());

                var tsPref = new TSPreference();
                var hookers = new ArrayList<IHooker>();
                hookers.add(tsPref);
                var matchers = new ArrayList<Obfuscated>();
                matchers.add(tsPref);
                matchers.add(new TbDialog());
                matchers.add(new TbToast());
                for (var entry : Preferences.getAll().entrySet()) {
                    try {
                        var hooker = maybeInitHooker(entry);
                        if (hooker != null) {
                            if (Boolean.FALSE != entry.getValue()) {
                                hookers.add(hooker);
                            }
                            if (hooker instanceof Obfuscated) {
                                matchers.add((Obfuscated) hooker);
                            }
                        }
                    } catch (Throwable tr) {
                        XposedBridge.log(tr);
                        sExceptions.put(entry.getKey(), tr);
                    }
                }

                if (DeobfuscationHelper.isVersionChanged(getContext())) {
                    if ("com.baidu.tieba".equals(lpparam.processName)) {
                        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                var activity = (Activity) param.thisObject;
                                var intent = new Intent(activity, XposedHelpers.findClass("com.baidu.tieba.LogoActivity", sClassLoader));
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                activity.startActivity(intent);
                            }
                        });
                    }
                    XposedBridge.log("Deobfuscation");
                    new DeobfuscationHook(
                            matchers.stream()
                                    .map(Obfuscated::matchers)
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toList())
                    ).hook();
                    return;
                }
                var lostList = matchers.stream()
                        .map(Obfuscated::matchers)
                        .flatMap(Collection::stream)
                        .map(Matcher::toString)
                        .filter(matcher -> !AcRules.isRuleFound(matcher))
                        .collect(Collectors.toList());
                if (!lostList.isEmpty()) {
                    XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            var activity = (Activity) param.thisObject;
                            var messages = new ArrayList<String>();
                            messages.add(Constants.getStrings().get("exception_rules_incomplete"));
                            messages.add(String.format(Locale.CHINA, "tbversion: %s, module version: %d",
                                    DeobfuscationHelper.getTbVersion(getContext()), BuildConfig.VERSION_CODE));
                            messages.add(String.format(Locale.CHINA, "%d rule(s) lost: %s", lostList.size(), lostList));
                            var message = TextUtils.join("\n", messages);
                            XposedBridge.log(message);
                            new AlertDialog.Builder(activity, DisplayUtils.isLightMode(getContext()) ?
                                    AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                                    .setTitle("警告").setMessage(message).setCancelable(false)
                                    .setNegativeButton(activity.getString(android.R.string.cancel), null)
                                    .setPositiveButton(activity.getString(android.R.string.ok), (dialogInterface, i) -> {
                                        Preferences.putSignature(0);
                                        DeobfuscationHelper.saveAndRestart(activity, "unknown", null);
                                    })
                                    .show();
                        }
                    });
                    return;
                }
                new Adp();
                for (var hooker : hookers) {
                    hooker.hook();
                }
            }

            private IHooker maybeInitHooker(Map.Entry<String, ?> entry) {
                switch (entry.getKey()) {
                    case "home_recommend":
                    case "fragment_tab":
                        return new FragmentTab();
                    case "switch_manager":
                        return new SwitchManager();
                    case "purge":
                        return new Purge();
                    case "purge_my":
//                        return new PurgeMy();
                    case "red_tip":
                        return new RedTip();
                    case "follow_filter":
                        return new FollowFilter();
                    case "personalized_filter":
                        return new PersonalizedFilter();
                    case "content_filter":
                        return new ContentFilter();
                    case "frs_page_filter":
                        return new FrsPageFilter();
                    case "create_view":
                        return new CreateView();
                    case "thread_store":
                        return new ThreadStore();
                    case "history_cache":
                        return new HistoryCache();
                    case "new_sub":
                        return new NewSub();
                    case "ripple":
                        return new Ripple();
                    case "save_images":
                        return new SaveImages();
                    case "my_attention":
                        return new MyAttention();
                    case "auto_sign":
                        return new AutoSign();
                    case "open_sign":
                        return new OpenSign();
                    case "eyeshield_mode":
                        return new EyeshieldMode();
                    case "origin_src":
                        return new OriginSrc();
                    case "redirect_image":
                        return new RedirectImage();
                    case "forbid_gesture":
                        return new ForbidGesture();
                    case "agree_num":
                        return new AgreeNum();
                    case "frs_tab":
                        return new FrsTab();
                    case "hide":
                        return new Hide();
                    case "hide_native":
                        if ((Boolean) entry.getValue()) {
                            try {
                                System.loadLibrary("hide");
                            } catch (UnsatisfiedLinkError e) {
                                XposedBridge.log(e);
                                Preferences.remove(entry.getKey());
                            }
                        }
                        break;
                    case "check_stack_trace":
                        return new StackTrace();
                    case "check_xposed":
                    case "check_module":
                        // prevent from being removed
                        break;
                    default:
                        Preferences.remove(entry.getKey());
                        break;
                }
                return null;
            }
        });
    }
}
