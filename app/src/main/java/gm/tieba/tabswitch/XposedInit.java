package gm.tieba.tabswitch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.hooker.AntiConfusion;
import gm.tieba.tabswitch.hooker.AntiConfusionHelper;
import gm.tieba.tabswitch.hooker.TSPreference;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.Preferences;
import gm.tieba.tabswitch.hooker.model.Rule;
import gm.tieba.tabswitch.hooker.model.TbDialogBuilder;

public class XposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static String sPath;
    private Resources mRes;

    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("DiscouragedPrivateApi")
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        sPath = startupParam.modulePath;
        AssetManager assetManager = AssetManager.class.newInstance();
        AssetManager.class.getDeclaredMethod("addAssetPath", String.class).invoke(assetManager, sPath);
        mRes = new Resources(assetManager, null, null);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        ClassLoader classLoader = lpparam.classLoader;
        if (lpparam.packageName.equals("com.baidu.tieba") || XposedHelpers.findClassIfExists("com.baidu.tieba.tblauncher.MainTabActivity", lpparam.classLoader) != null) {
            XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!(param.args[0] instanceof Application)) return;
                    Context context = ((Application) param.args[0]).getApplicationContext();
                    Preferences.init(context);
                    try {
                        Rule.init(context);
                        List<String> lostList = AntiConfusionHelper.getRulesLost();
                        if (lostList.size() != 0) {
                            throw new SQLiteException("rules incomplete, current version: " + AntiConfusionHelper.getTbVersion(context) + ", lost " + lostList.size() + " rule(s): " + lostList.toString());
                        }
                    } catch (SQLiteException e) {
                        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                Activity activity = (Activity) param.thisObject;
                                if (!Preferences.getIsEULAAccepted()) return;
                                XposedBridge.log(e.toString());
                                if (Rule.isRuleFound("Lcom/baidu/tieba/R$layout;->dialog_bdalert:I")) {
                                    TbDialogBuilder bdAlert = new TbDialogBuilder(classLoader, activity, "警告",
                                            "规则异常，建议您执行反混淆。若执行完后仍出现此对话框则应更新模块，若模块已是最新版本则应向作者反馈。\n" + e.toString(), false, null);
                                    bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
                                    bdAlert.setOnYesButtonClickListener(v -> AntiConfusionHelper.saveAndRestart(activity, "unknown", null));
                                    bdAlert.show();
                                } else {
                                    AlertDialog alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                                            .setTitle("警告").setMessage("规则异常，建议您执行反混淆。若执行完后仍出现此对话框则应更新模块，若模块已是最新版本则应向作者反馈。\n" + e.toString()).setCancelable(false)
                                            .setNegativeButton("取消", (dialogInterface, i) -> {
                                            }).setPositiveButton("确定", (dialogInterface, i) -> AntiConfusionHelper.saveAndRestart(activity, "unknown", null)).create();
                                    alertDialog.show();
                                }
                            }
                        });
                    }
                    if (AntiConfusionHelper.isVersionChanged(context)) {
                        new AntiConfusion(classLoader, mRes).hook();
                        return;
                    }

                    new TSPreference(classLoader, mRes).hook();
                    for (Map.Entry<String, ?> entry : Preferences.getAll().entrySet()) {
                        BaseHooker.init(classLoader, context, mRes, entry);
                    }
                }
            });
        } else switch (lpparam.packageName) {
            case "android":
                break;
            case "com.baidu.netdisk":
                XposedHelpers.findAndHookMethod("com.baidu.netdisk.ui.Navigate", classLoader, "initFlashFragment", XC_MethodReplacement.returnConstant(null));
                XposedHelpers.findAndHookMethod("com.baidu.netdisk.ui.advertise.FlashAdvertiseActivity", classLoader, "initFlashFragment", XC_MethodReplacement.returnConstant(null));
                XposedHelpers.findAndHookMethod("com.baidu.netdisk.ui.transfer.TransferListTabActivity", classLoader, "initYouaGuideView", XC_MethodReplacement.returnConstant(null));
                // "show or close "
                for (Method method : classLoader.loadClass("com.baidu.netdisk.homepage.ui.card.____").getDeclaredMethods()) {
                    if (Arrays.toString(method.getParameterTypes()).equals("[boolean]")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
                    }
                }

                XposedHelpers.findAndHookMethod("com.baidu.netdisk.media.video.source.NormalVideoSource", classLoader, "getAdTime", XC_MethodReplacement.returnConstant(0));
                XposedHelpers.findAndHookMethod("com.baidu.netdisk.preview.video.model._", classLoader, "getAdTime", XC_MethodReplacement.returnConstant(0));

                for (Method method : classLoader.loadClass("com.baidu.netdisk.media.speedup.SpeedUpModle").getDeclaredMethods()) {
                    if (method.getReturnType().equals(boolean.class)) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true));
                    }
                }
                break;
            case "com.coolapk.market":
                XposedHelpers.findAndHookMethod(String.class, "format", String.class, Object[].class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0].equals("https://%s%s")) {
                            Object[] objects = (Object[]) param.args[1];
                            if (objects.length == 2 && objects[1].equals("/api/ad/union/sdk/get_ads/")) {
                                param.setResult(null);
                            }
                        }
                    }
                });
                XposedHelpers.findAndHookConstructor(File.class, String.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (Objects.equals(param.args[1], "gdt_plugin.jar")) {
                            XposedHelpers.setObjectField(param.thisObject, "path", null);
                        }
                    }
                });
                try {
                    XposedHelpers.findAndHookMethod("com.coolapk.market.model.$$AutoValue_Feed", classLoader, "getDetailSponsorCard", XC_MethodReplacement.returnConstant(null));
                    XposedBridge.hookAllConstructors(XposedHelpers.findClass("com.coolapk.market.model.AutoValue_Feed", classLoader), new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.args[142] = null;
                        }
                    });
                } catch (XposedHelpers.ClassNotFoundError ignored) {
                }
                break;
        }
    }
}
