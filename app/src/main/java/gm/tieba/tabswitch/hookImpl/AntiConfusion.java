package gm.tieba.tabswitch.hookImpl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;

import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import bin.zip.ZipEntry;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.RulesDbHelper;
import gm.tieba.tabswitch.util.IO;

public class AntiConfusion extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.launcherGuide.tblauncher.GuideActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @SuppressLint({"ApplySharedPref"})
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = ((Activity) param.thisObject);
                SharedPreferences tsConfig = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                SharedPreferences sharedPreferences = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
                if (tsConfig.getString("anti-confusion_version", "unknown").equals(sharedPreferences.getString("key_rate_version", "unknown"))
                        && !sharedPreferences.getString("key_rate_version", "unknown").equals("unknown"))
                    return;
                TextView textView = new TextView(activity);
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                textView.setText(String.format("读取%s", "ZipEntry"));
                AlertDialog alertDialog;
                if ((activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                    textView.setTextColor(Hook.modRes.getColor(R.color.colorPrimary, null));
                    alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                            .setTitle("贴吧TS反混淆").setMessage("正在定位被混淆的类和方法，请耐心等待").setView(textView).setCancelable(false).create();
                } else {
                    textView.setTextColor(Hook.modRes.getColor(R.color.colorPrimaryDark, null));
                    alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                            .setTitle("贴吧TS反混淆").setMessage("正在定位被混淆的类和方法，请耐心等待").setView(textView).setCancelable(false).create();
                }
                alertDialog.show();
                new Thread(() -> {
                    File dexDir = new File(activity.getCacheDir().getAbsolutePath(), "dex");
                    try {
                        if (dexDir.exists()) IO.deleteFiles(dexDir);
                        dexDir.mkdirs();
                        bin.zip.ZipFile zipFile = new bin.zip.ZipFile(new File(activity.getPackageResourcePath()));
                        Enumeration<ZipEntry> enumeration = zipFile.getEntries();
                        int dexCount = 0;
                        while (enumeration.hasMoreElements()) {
                            ZipEntry ze = enumeration.nextElement();
                            if (ze.getName().matches("classes[0-9]*?\\.dex")) {
                                dexCount++;
                                int finalDexCount = dexCount;
                                activity.runOnUiThread(() -> textView.setText(String.format(Locale.CHINA, "解压第%d个dex", finalDexCount)));
                                IO.copyFileFromStream(zipFile.getInputStream(ze), dexDir + File.separator + ze.getName());
                            }
                        }
                        //新建数据库
                        activity.deleteDatabase("Rules.db");
                        new RulesDbHelper(activity.getApplicationContext()).getReadableDatabase();
                        SQLiteDatabase db = activity.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null);
                        ruleMapList = AntiConfusionHelper.convertDbToMapList(db);

                        File[] fs = dexDir.listFiles();
                        int searched = 0;
                        String searchProgress = "";
                        String mainProgress = "总进度：" + searched + "/" + ruleMapList.size();
                        for (int i = 0; i < fs.length; i++) {
                            DexFile dex = new DexFile(fs[i]);
                            List<ClassDefItem> classes = dex.ClassDefsSection.getItems();
                            for (int j = 0; j < classes.size(); j++) {
                                int clsProgress = 100 * j / classes.size() / fs.length + 100 * i / fs.length;
                                searchProgress = "搜索进度：" + clsProgress + "%";
                                String finalSearchProgress = searchProgress;
                                String finalMainProgress = mainProgress;
                                activity.runOnUiThread(() -> textView.setText(String.format("%s\n%s", finalSearchProgress, finalMainProgress)));

                                String signature = classes.get(j).getClassType().getTypeDescriptor();//类签名
                                if (!signature.startsWith("Le/b/") && !signature.startsWith("Lcom/baidu/tieba/") && !signature.startsWith("Lcom/baidu/tbadk/core/util/"))
                                    continue;
                                if (AntiConfusionHelper.searchAndUpdate(classes.get(j), 0) || AntiConfusionHelper.searchAndUpdate(classes.get(j), 1)) {
                                    searched++;
                                    mainProgress = "总进度：" + searched + "/" + ruleMapList.size();
                                    if (searched == ruleMapList.size()) break;
                                    String finalSearchProgress1 = searchProgress;
                                    String finalMainProgress1 = mainProgress;
                                    activity.runOnUiThread(() -> textView.setText(String.format("%s\n%s", finalSearchProgress1, finalMainProgress1)));
                                }
                            }
                            if (searched == ruleMapList.size()) break;
                        }
                        String finalSearchProgress2 = searchProgress;
                        String finalMainProgress2 = mainProgress;
                        activity.runOnUiThread(() -> textView.setText(String.format("%s\n%s\n反混淆完成，即将重启", finalSearchProgress2, finalMainProgress2)));
                        AntiConfusionHelper.putMapListToDb(db);
                        SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
                        if (tsPreference.getBoolean("clean_dir", false)) {
                            IO.deleteFiles(activity.getFilesDir());
                            IO.deleteFiles(activity.getCacheDir());
                            IO.deleteFiles(new File(activity.getCacheDir().getAbsolutePath() + "image"));
                            IO.deleteFiles(activity.getExternalCacheDir());
                        } else IO.deleteFiles(dexDir);
                        XposedBridge.log("anti-confusion accomplished, last version: " + tsConfig.getString("anti-confusion_version", "unknown")
                                + ", current version: " + sharedPreferences.getString("key_rate_version", "unknown"));
                        SharedPreferences.Editor editor = tsConfig.edit();
                        editor.putString("anti-confusion_version", sharedPreferences.getString("key_rate_version", "unknown"));
                        editor.commit();
                        //重启
                        Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        activity.startActivity(intent);
                        System.exit(0);
                    } catch (Throwable throwable) {
                        activity.runOnUiThread(() -> textView.setText(String.format("处理失败\n%s", Log.getStackTraceString(throwable))));
                        XposedBridge.log(throwable);
                    }
                }).start();
            }
        });
    }
}