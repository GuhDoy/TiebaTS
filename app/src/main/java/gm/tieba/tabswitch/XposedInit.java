package gm.tieba.tabswitch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Adp;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.TSPreference;
import gm.tieba.tabswitch.hooker.add.CreateView;
import gm.tieba.tabswitch.hooker.add.HistoryCache;
import gm.tieba.tabswitch.hooker.add.MyAttention;
import gm.tieba.tabswitch.hooker.add.NewSub;
import gm.tieba.tabswitch.hooker.add.Ripple;
import gm.tieba.tabswitch.hooker.add.SaveImages;
import gm.tieba.tabswitch.hooker.add.ThreadStore;
import gm.tieba.tabswitch.hooker.anticonfusion.AntiConfusion;
import gm.tieba.tabswitch.hooker.anticonfusion.AntiConfusionHelper;
import gm.tieba.tabswitch.hooker.auto.AutoSign;
import gm.tieba.tabswitch.hooker.auto.EyeshieldMode;
import gm.tieba.tabswitch.hooker.auto.FrsTab;
import gm.tieba.tabswitch.hooker.auto.OpenSign;
import gm.tieba.tabswitch.hooker.auto.OriginSrc;
import gm.tieba.tabswitch.hooker.eliminate.ContentFilter;
import gm.tieba.tabswitch.hooker.eliminate.FollowFilter;
import gm.tieba.tabswitch.hooker.eliminate.FragmentTab;
import gm.tieba.tabswitch.hooker.eliminate.FrsPageFilter;
import gm.tieba.tabswitch.hooker.eliminate.PersonalizedFilter;
import gm.tieba.tabswitch.hooker.eliminate.Purify;
import gm.tieba.tabswitch.hooker.eliminate.PurifyEnter;
import gm.tieba.tabswitch.hooker.eliminate.PurifyMy;
import gm.tieba.tabswitch.hooker.eliminate.RedTip;
import gm.tieba.tabswitch.hooker.eliminate.SwitchManager;
import gm.tieba.tabswitch.hooker.extra.ForbidGesture;
import gm.tieba.tabswitch.hooker.extra.Hide;
import gm.tieba.tabswitch.hooker.extra.RedirectImage;
import gm.tieba.tabswitch.hooker.extra.StackTrace;
import gm.tieba.tabswitch.widget.TbDialog;

public class XposedInit extends XposedContext implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        sPath = startupParam.modulePath;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.baidu.tieba") && XposedHelpers.findClassIfExists(
                "com.baidu.tieba.tblauncher.MainTabActivity", lpparam.classLoader) == null) return;
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!(param.args[0] instanceof Application)) return;
                attachBaseContext((Application) param.args[0]);
                sClassLoader = lpparam.classLoader;
                Preferences.init(getContext());
                AcRules.init(getContext());
                if (AntiConfusionHelper.isVersionChanged(getContext())) {
                    XposedBridge.log("AntiConfusion");
                    new AntiConfusion().hook();
                    return;
                }
                var lostList = AntiConfusionHelper.getRulesLost();
                if (!lostList.isEmpty()) {
                    XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            var activity = (Activity) param.thisObject;
                            var messages = new ArrayList<String>();
                            messages.add(Constants.getStrings().get("exception_rules_incomplete"));
                            messages.add(String.format(Locale.CHINA, "tbversion: %s, module version: %d",
                                    AntiConfusionHelper.getTbVersion(getContext()), BuildConfig.VERSION_CODE));
                            messages.add(String.format(Locale.CHINA, "lost %d rule(s): %s", lostList.size(), lostList));
                            var message = TextUtils.join("\n", messages);
                            XposedBridge.log(message);
                            if (AcRules.isRuleFound(Constants.getMatchers().get(TbDialog.class))) {
                                var bdAlert = new TbDialog(activity, "警告", message, false, null);
                                bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
                                bdAlert.setOnYesButtonClickListener(v -> AntiConfusionHelper
                                        .saveAndRestart(activity, "unknown", null));
                                bdAlert.show();
                            } else {
                                new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                        .setTitle("警告").setMessage(message).setCancelable(false)
                                        .setNegativeButton(activity.getString(android.R.string.cancel), null)
                                        .setPositiveButton(activity.getString(android.R.string.ok), (dialogInterface, i) -> AntiConfusionHelper
                                                .saveAndRestart(activity, "unknown", null))
                                        .show();
                            }
                        }
                    });
                    return;
                }

                new TSPreference().hook();
                new Adp();
                for (var entry : Preferences.getAll().entrySet()) {
                    try {
                        initHooker(entry);
                    } catch (Throwable tr) {
                        XposedBridge.log(tr);
                        sExceptions.put(entry.getKey(), tr);
                    }
                }
            }

            private void initHooker(Map.Entry<String, ?> entry) throws Throwable {
                switch (entry.getKey()) {
                    case "home_recommend":
                    case "fragment_tab":
                        new FragmentTab().hook();
                        break;
                    case "switch_manager":
                        new SwitchManager().hook();
                        break;
                    case "purify":
                        if ((Boolean) entry.getValue()) new Purify().hook();
                        break;
                    case "purify_enter":
                        if ((Boolean) entry.getValue()) new PurifyEnter().hook();
                        break;
                    case "purify_my":
                        if ((Boolean) entry.getValue()) new PurifyMy().hook();
                        break;
                    case "red_tip":
                        if ((Boolean) entry.getValue()) new RedTip().hook();
                        break;
                    case "follow_filter":
                        if ((Boolean) entry.getValue()) new FollowFilter().hook();
                        break;
                    case "personalized_filter":
                        new PersonalizedFilter().hook();
                        break;
                    case "content_filter":
                        new ContentFilter().hook();
                        break;
                    case "frs_page_filter":
                        new FrsPageFilter().hook();
                        break;
                    case "create_view":
                        if ((Boolean) entry.getValue()) new CreateView().hook();
                        break;
                    case "thread_store":
                        if ((Boolean) entry.getValue()) new ThreadStore().hook();
                        break;
                    case "history_cache":
                        if ((Boolean) entry.getValue()) new HistoryCache().hook();
                        break;
                    case "new_sub":
                        if ((Boolean) entry.getValue()) new NewSub().hook();
                        break;
                    case "ripple":
                        if ((Boolean) entry.getValue()) new Ripple().hook();
                        break;
                    case "save_images":
                        if ((Boolean) entry.getValue()) new SaveImages().hook();
                        break;
                    case "my_attention":
                        if ((Boolean) entry.getValue()) new MyAttention().hook();
                        break;
                    case "auto_sign":
                        if ((Boolean) entry.getValue()) new AutoSign().hook();
                        break;
                    case "open_sign":
                        if ((Boolean) entry.getValue()) new OpenSign().hook();
                        break;
                    case "eyeshield_mode":
                        if ((Boolean) entry.getValue()) new EyeshieldMode().hook();
                        break;
                    case "origin_src":
                        if ((Boolean) entry.getValue()) new OriginSrc().hook();
                        break;
                    case "redirect_image":
                        if ((Boolean) entry.getValue()) new RedirectImage().hook();
                        break;
                    case "forbid_gesture":
                        if ((Boolean) entry.getValue()) new ForbidGesture().hook();
                        break;
                    case "agree_num":
                        if (!(Boolean) entry.getValue()) break;
                        XposedHelpers.findAndHookMethod("tbclient.Agree$Builder", sClassLoader,
                                "build", boolean.class, new XC_MethodHook() {
                                    @Override
                                    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        XposedHelpers.setObjectField(param.thisObject, "agree_num",
                                                XposedHelpers.getObjectField(param.thisObject, "diff_agree_num"));
                                    }
                                });
                        break;
                    case "frs_tab":
                        if ((Boolean) entry.getValue()) new FrsTab().hook();
                        break;
                    case "hide":
                        if ((Boolean) entry.getValue()) new Hide().hook();
                        break;
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
                        if ((Boolean) entry.getValue()) new StackTrace().hook();
                        break;
                    case "check_xposed":
                    case "check_module":
                        // prevent from being removed
                        break;
                    default:
                        Preferences.remove(entry.getKey());
                        break;
                }
            }
        });
    }
}
