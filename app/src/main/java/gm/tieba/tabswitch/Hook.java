package gm.tieba.tabswitch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.hookImpl.AntiConfusion;
import gm.tieba.tabswitch.hookImpl.AntiConfusionHelper;
import gm.tieba.tabswitch.hookImpl.TSPreference;
import gm.tieba.tabswitch.hookImpl.TSPreferenceHelper;

public class Hook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static Resources modRes;
    public static List<Map<String, String>> ruleMapList = Collections.emptyList();
    public static String BDUSS;
    public static Set<String> follow;
    public static Map<String, ?> preferenceMap;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID))
            XposedHelpers.findAndHookMethod(BuildConfig.APPLICATION_ID + ".ui.MainActivity", lpparam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
        else if (lpparam.packageName.equals("com.baidu.tieba") || XposedHelpers.findClassIfExists("com.baidu.tieba.tblauncher.MainTabActivity", lpparam.classLoader) != null) {
            XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[0] instanceof Application) {
                        ClassLoader classLoader = lpparam.classLoader;
                        Context context = ((Application) param.args[0]).getApplicationContext();

                        try {
                            SQLiteDatabase db = context.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null);
                            ruleMapList = AntiConfusionHelper.convertDbToMapList(db);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && context.getPackageManager().getPackageInfo(context.getPackageName(), 0).getLongVersionCode() < 201523200 ||
                                    Build.VERSION.SDK_INT < Build.VERSION_CODES.P && context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode < 201523200)
                                AntiConfusionHelper.matcherList.remove("\"custom_ext_data\"");
                            List<String> lostList = AntiConfusionHelper.getLostList();
                            if (lostList.size() != 0)
                                throw new SQLiteException("rules incomplete, current version: " + AntiConfusionHelper.getTbVersion(context) + ", lost " + lostList.size() + " rule(s): " + lostList.toString());
                        } catch (SQLiteException e) {
                            XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    XposedBridge.log(e.toString());
                                    Activity activity = (Activity) param.thisObject;
                                    SharedPreferences tsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                                    if (tsConfig.getBoolean("EULA", false))
                                        try {
                                            TSPreferenceHelper.TbDialogBuilder bdalert = new TSPreferenceHelper.TbDialogBuilder(classLoader, activity, "警告",
                                                    "规则异常，建议您执行反混淆。若执行完后仍出现此对话框则应更新模块，若模块已是最新版本则应向作者反馈。\n" + e.toString(), false, null);
                                            bdalert.setOnNoButtonClickListener(v -> bdalert.dismiss());
                                            bdalert.setOnYesButtonClickListener(v -> AntiConfusionHelper.saveAndRestart(activity, "unknown", null));
                                            bdalert.show();
                                        } catch (NullPointerException e2) {
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
                            AntiConfusion.hook(classLoader);
                            return;
                        }

                        TSPreference.hook(classLoader);
                        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.data.AccountData", classLoader, "getBDUSS", new XC_MethodHook() {
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                BDUSS = String.valueOf(param.getResult());
                            }
                        });
                        SharedPreferences tsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                        follow = tsConfig.getStringSet("follow", null);
                        SharedPreferences tsPreference = context.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
                        preferenceMap = tsPreference.getAll();
                        for (Map.Entry<String, ?> entry : preferenceMap.entrySet())
                            HookDispatcher.hook(classLoader, entry, context);
                    }
                }
            });
        } else if (lpparam.packageName.equals("com.baidu.netdisk")) {
            ClassLoader classLoader = lpparam.classLoader;
            XposedHelpers.findAndHookMethod("com.baidu.netdisk.ui.Navigate", classLoader, "initFlashFragment", XC_MethodReplacement.returnConstant(null));
            XposedHelpers.findAndHookMethod("com.baidu.netdisk.ui.advertise.FlashAdvertiseActivity", classLoader, "initFlashFragment", XC_MethodReplacement.returnConstant(null));
            XposedHelpers.findAndHookMethod("com.baidu.netdisk.ui.preview.video.source.NormalVideoSource", classLoader, "getAdTime", XC_MethodReplacement.returnConstant(0));
            XposedHelpers.findAndHookMethod("com.baidu.netdisk.preview.video.model._", classLoader, "getAdTime", XC_MethodReplacement.returnConstant(0));
        } else {
            XposedHelpers.findAndHookMethod(String.class, "format", String.class, Object[].class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (((String) param.args[0]).equals("https://%s%s")) {
                        Object[] objects = (Object[]) param.args[1];
                        if (objects.length == 2 && objects[1].equals("/api/ad/union/sdk/get_ads/"))
                            param.setResult(null);
                    }
                }
            });
            XposedHelpers.findAndHookConstructor(File.class, String.class, String.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (Objects.equals(param.args[1], "gdt_plugin.jar"))
                        XposedHelpers.setObjectField(param.thisObject, "path", null);
                }
            });
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        AssetManager assetManager = AssetManager.class.newInstance();
        AssetManager.class.getDeclaredMethod("addAssetPath", String.class).invoke(assetManager, startupParam.modulePath);
        modRes = new Resources(assetManager, null, null);
    }
}