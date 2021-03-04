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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
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
import gm.tieba.tabswitch.util.IO;

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
                            db.close();
                            List<String> ruleList = new ArrayList<>();
                            for (int i = 0; i < ruleMapList.size(); i++) {
                                Map<String, String> map = ruleMapList.get(i);
                                ruleList.add(map.get("rule"));
                            }
                            if (!ruleList.containsAll(AntiConfusionHelper.matcherList)) {
                                SharedPreferences tsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                                if (tsConfig.getBoolean("EULA", false)) {
                                    SharedPreferences sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                                    List<String> lostList = new ArrayList<>();
                                    AntiConfusionHelper.addMatcher(lostList);
                                    lostList.removeAll(ruleList);
                                    throw new SQLiteException("rules incomplete, current version: " + sharedPreferences.getString("key_rate_version", "unknown") + ", lost " + lostList.size() + " rules: " + lostList.toString());
                                }
                            } else if (!AntiConfusionHelper.isNeedAntiConfusion(context))
                                AntiConfusionHelper.matcherList.clear();
                        } catch (SQLiteException e) {
                            XposedBridge.log(e.toString());
                            XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    Activity activity = (Activity) param.thisObject;
                                    @SuppressLint("ApplySharedPref") AlertDialog alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                                            .setTitle("警告").setMessage("规则不完整，建议您执行反混淆。若执行完后仍出现此对话框则应该更新模块，若模块已是最新版本则应该向作者反馈。\n" + Log.getStackTraceString(e)).setCancelable(true)
                                            .setNegativeButton("取消", (dialogInterface, i) -> {
                                            }).setPositiveButton("确定", (dialogInterface, i) -> {
                                                SharedPreferences tsConfig = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                                                SharedPreferences.Editor editor = tsConfig.edit();
                                                editor.putString("anti-confusion_version", "unknown");
                                                editor.commit();
                                                Intent intent = new Intent();
                                                intent.setClassName(activity, "com.baidu.tieba.launcherGuide.tblauncher.GuideActivity");
                                                activity.startActivity(intent);
                                            }).create();
                                    alertDialog.show();
                                    IO.copyFile(new FileInputStream(activity.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null).getPath()),
                                            new FileOutputStream(new File(activity.getExternalFilesDir(null), "Rules.db")));
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