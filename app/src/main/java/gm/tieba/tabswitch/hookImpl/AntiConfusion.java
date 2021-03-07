package gm.tieba.tabswitch.hookImpl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;

import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import bin.zip.ZipEntry;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.RulesDbHelper;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.IO;

public class AntiConfusion extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.launcherGuide.tblauncher.GuideActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @SuppressLint({"ApplySharedPref"})
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = ((Activity) param.thisObject);
                if (!AntiConfusionHelper.isNeedAntiConfusion(activity)) return;
                TextView textView = new TextView(activity);
                textView.setTextSize(14);
                textView.setPadding(0, 5, 0, 5);
                textView.setText(String.format("读取%s", "ZipEntry"));
                TextView progressBackground = new TextView(activity);
                progressBackground.setBackgroundColor(Hook.modRes.getColor(R.color.colorProgress, null));
                RelativeLayout progressContainer = new RelativeLayout(activity);
                progressContainer.addView(progressBackground);
                progressContainer.addView(textView);
                RelativeLayout.LayoutParams tvLp = (RelativeLayout.LayoutParams) textView.getLayoutParams();
                tvLp.addRule(RelativeLayout.CENTER_IN_PARENT);
                textView.setLayoutParams(tvLp);
                RelativeLayout.LayoutParams rlLp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                progressContainer.setLayoutParams(rlLp);
                AlertDialog alertDialog;
                if (DisplayHelper.isLightMode(activity)) {
                    textView.setTextColor(Hook.modRes.getColor(R.color.colorPrimaryDark, null));
                    alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                            .setTitle("贴吧TS反混淆").setMessage("正在定位被混淆的类和方法，请耐心等待").setView(progressContainer).setCancelable(false).create();
                } else {
                    textView.setTextColor(Hook.modRes.getColor(R.color.colorPrimary, null));
                    alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                            .setTitle("贴吧TS反混淆").setMessage("正在定位被混淆的类和方法，请耐心等待").setView(progressContainer).setCancelable(false).create();
                }
                alertDialog.show();
                new Thread(() -> {
                    File dexDir = new File(activity.getCacheDir().getAbsolutePath(), "dex");
                    try {
                        if (dexDir.exists()) IO.deleteFiles(dexDir);
                        dexDir.mkdirs();
                        bin.zip.ZipFile zipFile = new bin.zip.ZipFile(new File(activity.getPackageResourcePath()));
                        Enumeration<ZipEntry> enumeration = zipFile.getEntries();
                        int unzippedCount = 0;
                        int entrySize = zipFile.getEntrySize();
                        while (enumeration.hasMoreElements()) {
                            unzippedCount++;
                            float progress = (float) unzippedCount / entrySize;
                            activity.runOnUiThread(() -> {
                                textView.setText("解压");
                                ViewGroup.LayoutParams lp = progressBackground.getLayoutParams();
                                lp.height = textView.getHeight();
                                lp.width = (int) (progressContainer.getWidth() * progress);
                                progressBackground.setLayoutParams(lp);
                            });

                            ZipEntry ze = enumeration.nextElement();
                            if (ze.getName().matches("classes[0-9]*?\\.dex"))
                                IO.copyFile(zipFile.getInputStream(ze), new File(dexDir, ze.getName()));
                        }
                        //新建数据库
                        activity.deleteDatabase("Rules.db");
                        new RulesDbHelper(activity.getApplicationContext()).getReadableDatabase();
                        SQLiteDatabase db = activity.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null);

                        File[] fs = dexDir.listFiles();
                        Arrays.sort(fs);
                        for (int i = 0; i < fs.length; i++) {
                            DexFile dex = new DexFile(fs[i]);
                            List<ClassDefItem> classes = dex.ClassDefsSection.getItems();
                            for (int j = 0; j < classes.size(); j++) {
                                float clsProgress = (float) j / fs.length / classes.size() + (float) i / fs.length;
                                activity.runOnUiThread(() -> {
                                    textView.setText("搜索");
                                    ViewGroup.LayoutParams lp = progressBackground.getLayoutParams();
                                    lp.height = textView.getHeight();
                                    lp.width = (int) (progressContainer.getWidth() * clsProgress);
                                    progressBackground.setLayoutParams(lp);
                                });

                                String signature = classes.get(j).getClassType().getTypeDescriptor();//类签名
                                if (!signature.startsWith("Le/b/") && !signature.startsWith("Lcom/baidu/"))
                                    continue;
                                AntiConfusionHelper.searchAndSave(classes.get(j), 0, db);
                                AntiConfusionHelper.searchAndSave(classes.get(j), 1, db);
                            }
                        }
                        SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
                        if (tsPreference.getBoolean("clean_dir", false)) {
                            IO.deleteFiles(activity.getFilesDir());
                            IO.deleteFiles(activity.getCacheDir());
                            IO.deleteFiles(new File(activity.getCacheDir().getAbsolutePath() + "image"));
                            IO.deleteFiles(activity.getExternalCacheDir());
                        } else IO.deleteFiles(dexDir);
                        new File(activity.getExternalFilesDir(null), "Rules.db").delete();
                        SharedPreferences tsConfig = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                        SharedPreferences sharedPreferences = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
                        XposedBridge.log("anti-confusion accomplished, last version: " + tsConfig.getString("anti-confusion_version", "unknown")
                                + ", current version: " + sharedPreferences.getString("key_rate_version", "unknown"));
                        SharedPreferences.Editor editor = tsConfig.edit();
                        editor.putString("anti-confusion_version", sharedPreferences.getString("key_rate_version", "unknown"));
                        editor.commit();
                        Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                        activity.finishAffinity();
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