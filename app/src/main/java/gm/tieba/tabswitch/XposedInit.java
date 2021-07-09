package gm.tieba.tabswitch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.List;
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
import gm.tieba.tabswitch.hooker.AntiConfusion;
import gm.tieba.tabswitch.hooker.AntiConfusionHelper;
import gm.tieba.tabswitch.hooker.TSPreference;
import gm.tieba.tabswitch.hooker.add.CreateView;
import gm.tieba.tabswitch.hooker.add.HistoryCache;
import gm.tieba.tabswitch.hooker.add.MyAttention;
import gm.tieba.tabswitch.hooker.add.NewSub;
import gm.tieba.tabswitch.hooker.add.Ripple;
import gm.tieba.tabswitch.hooker.add.SaveImages;
import gm.tieba.tabswitch.hooker.add.ThreadStore;
import gm.tieba.tabswitch.hooker.auto.AutoSign;
import gm.tieba.tabswitch.hooker.auto.EyeshieldMode;
import gm.tieba.tabswitch.hooker.auto.FrsTab;
import gm.tieba.tabswitch.hooker.auto.OpenSign;
import gm.tieba.tabswitch.hooker.auto.OriginSrc;
import gm.tieba.tabswitch.hooker.extra.ForbidGesture;
import gm.tieba.tabswitch.hooker.extra.Hide;
import gm.tieba.tabswitch.hooker.extra.RedirectImage;
import gm.tieba.tabswitch.hooker.extra.StackTrace;
import gm.tieba.tabswitch.hooker.minus.ContentFilter;
import gm.tieba.tabswitch.hooker.minus.FollowFilter;
import gm.tieba.tabswitch.hooker.minus.FragmentTab;
import gm.tieba.tabswitch.hooker.minus.FrsPageFilter;
import gm.tieba.tabswitch.hooker.minus.PersonalizedFilter;
import gm.tieba.tabswitch.hooker.minus.Purify;
import gm.tieba.tabswitch.hooker.minus.PurifyEnter;
import gm.tieba.tabswitch.hooker.minus.PurifyMy;
import gm.tieba.tabswitch.hooker.minus.RedTip;
import gm.tieba.tabswitch.hooker.minus.SwitchManager;
import gm.tieba.tabswitch.widget.TbDialog;

public class XposedInit extends XposedWrapper implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    @SuppressLint("DiscouragedPrivateApi")
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        sPath = startupParam.modulePath;
        AssetManager assetManager = AssetManager.class.newInstance();
        XposedHelpers.callMethod(assetManager, "addAssetPath", sPath);
        sRes = new Resources(assetManager, null, null);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.baidu.tieba") && XposedHelpers.findClassIfExists(
                "com.baidu.tieba.tblauncher.MainTabActivity", lpparam.classLoader) == null) return;
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!(param.args[0] instanceof Application)) return;
                sContextRef = new WeakReference<>(((Application) param.args[0]).getApplicationContext());
                sClassLoader = lpparam.classLoader;
                Preferences.init(getContext());
                AntiConfusionHelper.initMatchers(sRes);
                try {
                    AcRules.init(getContext());
                    List<String> lostList = AntiConfusionHelper.getRulesLost();
                    if (!lostList.isEmpty()) {
                        throw new SQLiteException(String.format(Locale.getDefault(),
                                "rules incomplete, tbversion: %s, module version: %d, lost %d rule(s): %s",
                                AntiConfusionHelper.getTbVersion(getContext()), BuildConfig.VERSION_CODE, lostList.size(), lostList.toString()));
                    }
                } catch (SQLiteException e) {
                    XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(e.toString());
                            if (!Preferences.getIsEULAAccepted()) return;
                            Activity activity = (Activity) param.thisObject;
                            String message = sRes.getString(R.string.rules_incomplete) + "\n" + e.getMessage();
                            if (AcRules.isRuleFound(sRes.getString(R.string.TbDialog))) {
                                TbDialog bdAlert = new TbDialog(activity, "警告", message, false, null);
                                bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
                                bdAlert.setOnYesButtonClickListener(v -> AntiConfusionHelper
                                        .saveAndRestart(activity, "unknown", null, sRes));
                                bdAlert.show();
                            } else {
                                @SuppressWarnings("deprecation")
                                AlertDialog alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                        .setTitle("警告").setMessage(message).setCancelable(false)
                                        .setNegativeButton(activity.getString(android.R.string.cancel), (dialogInterface, i) -> {
                                        }).setPositiveButton(activity.getString(android.R.string.ok), (dialogInterface, i) -> AntiConfusionHelper
                                                .saveAndRestart(activity, "unknown", null, sRes)).create();
                                alertDialog.show();
                            }
                        }
                    });
                }
                if (AntiConfusionHelper.isVersionChanged(getContext())) {
                    new AntiConfusion().hook();
                    return;
                }

                new TSPreference().hook();
                new Adp();
                for (Map.Entry<String, ?> entry : Preferences.getAll().entrySet()) {
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
                        case "check_stack_trace":
                            if ((Boolean) entry.getValue()) new StackTrace().hook();
                            break;
                        case "check_xposed":
                        case "check_module":
                            // prevent from being removed
                            break;
                        default:
                            if (!BuildConfig.DEBUG) Preferences.remove(entry.getKey());
                            break;
                    }
                }
            }
        });
    }
}
