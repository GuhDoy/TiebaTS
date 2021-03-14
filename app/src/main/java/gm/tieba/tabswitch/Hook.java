package gm.tieba.tabswitch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

public class Hook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static Resources modRes;
    public static List<Map<String, String>> ruleMapList = Collections.emptyList();
    public static String BDUSS;
    public static Set<String> follow;
    public static Map<String, ?> preferenceMap;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("gm.tieba.tabswitch")) {
            XposedHelpers.findAndHookMethod("gm.tieba.tabswitch.ui.MainActivity", lpparam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
            //System.load(modulePath.substring(0, modulePath.lastIndexOf('/')) + "/lib/arm/libcrust.so");
        } else if (lpparam.packageName.equals("com.baidu.tieba") || XposedHelpers.findClassIfExists("com.baidu.tieba.tblauncher.MainTabActivity", lpparam.classLoader) != null) {
            XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[0] instanceof Application) {
                        ClassLoader classLoader = lpparam.classLoader;
                        Context context = ((Application) param.args[0]).getApplicationContext();

                        AntiConfusion.hook(classLoader);
                        try {
                            SQLiteDatabase db = context.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null);
                            ruleMapList = AntiConfusionHelper.convertDbToMapList(db);
                            if (context.getPackageManager().getPackageInfo(context.getPackageName(), 0).getLongVersionCode() < 201523200)
                                AntiConfusionHelper.matcherList.remove("\"custom_ext_data\"");
                            List<String> lostList = AntiConfusionHelper.getLostList();
                            if (lostList.size() != 0)
                                throw new SQLiteException("rules incomplete, current version: " + AntiConfusionHelper.getTbVersion(context) + ", lost " + lostList.size() + " rule(s): " + lostList.toString());
                        } catch (SQLiteException e) {
                            XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    XposedBridge.log(e.toString());
                                    Activity activity = (Activity) param.thisObject;
                                    @SuppressLint("ApplySharedPref") AlertDialog alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                                            .setTitle("警告").setMessage("规则异常，建议您执行反混淆。若执行完后仍出现此对话框则应更新模块，若模块已是最新版本则应向作者反馈。\n" + Log.getStackTraceString(e)).setCancelable(true)
                                            .setNegativeButton("取消", (dialogInterface, i) -> {
                                            }).setPositiveButton("确定", (dialogInterface, i) -> {
                                                Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.launcherGuide.tblauncher.GuideActivity");
                                                activity.startActivity(intent);
                                            }).create();
                                    SharedPreferences tsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                                    if (tsConfig.getBoolean("EULA", false)) alertDialog.show();
                                }
                            });
                        }

                        TSPreference.hook(classLoader);
                        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.data.AccountData", classLoader, "getBDUSS", new XC_MethodHook() {
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                BDUSS = String.valueOf(param.getResult());
                            }
                        });
                        SharedPreferences tsCache = context.getSharedPreferences("TS_cache", Context.MODE_PRIVATE);
                        follow = tsCache.getStringSet("follow", null);
                        SharedPreferences tsPreference = context.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
                        preferenceMap = tsPreference.getAll();
                        for (Map.Entry<String, ?> entry : preferenceMap.entrySet())
                            HookDispatcher.hook(classLoader, entry, context);
                    }
                }
            });
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        AssetManager assetManager = AssetManager.class.newInstance();
        //将来可能需要freeReflection
        AssetManager.class.getDeclaredMethod("addAssetPath", String.class).invoke(assetManager, startupParam.modulePath);
        modRes = new Resources(assetManager, null, null);
    }
}