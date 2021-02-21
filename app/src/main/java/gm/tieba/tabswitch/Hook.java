package gm.tieba.tabswitch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.lang.reflect.Field;
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

public class Hook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static Resources modRes;
    public static List<Map<String, String>> ruleMapList;
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
                        Context context = ((Application) param.args[0]).getApplicationContext();
                        TSPreference.hook(lpparam);
                        AntiConfusion.hook(lpparam);

                        SQLiteDatabase db = context.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null);
                        ruleMapList = AntiConfusionHelper.convertDbToMapList(db);
                        db.close();
                        int totalRuleSize = ruleMapList.size();
                        for (int i = 0; i < ruleMapList.size(); i++) {
                            Map<String, String> map = ruleMapList.get(i);
                            if ("".equals(map.get("class")) || "".equals(map.get("method"))) {
                                ruleMapList.remove(i);
                                i--;
                            }
                        }
                        if (ruleMapList.size() != totalRuleSize) {
                            SharedPreferences sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                            String tsWarning = "TS warning: rules incomplete (" + ruleMapList.size() + "/" + totalRuleSize + "), current version: " + sharedPreferences.getString("key_rate_version", "unknown");
                            XposedBridge.log(tsWarning);
                            XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    Activity activity = (Activity) param.thisObject;
                                    Toast.makeText(activity, tsWarning, Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.data.AccountData", lpparam.classLoader, "getBDUSS", new XC_MethodHook() {
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                BDUSS = String.valueOf(param.getResult());
                            }
                        });

                        SharedPreferences tsCache = context.getSharedPreferences("TS_cache", Context.MODE_PRIVATE);
                        follow = tsCache.getStringSet("follow", null);

                        SharedPreferences tsPreference = context.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
                        preferenceMap = tsPreference.getAll();
                        for (Map.Entry<String, ?> entry : preferenceMap.entrySet())
                            HookDispatcher.hook(lpparam, entry);
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