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
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

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
                if (AntiConfusionHelper.isDexChanged(activity)) activity.deleteDatabase("Rules.db");
                else if (AntiConfusionHelper.getLostList().size() != 0)
                    AntiConfusionHelper.matcherList = AntiConfusionHelper.getLostList();
                else return;
                new RulesDbHelper(activity.getApplicationContext()).getReadableDatabase();

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
                        int entryCount = 0;
                        int entrySize = zipFile.getEntrySize();
                        while (enumeration.hasMoreElements()) {
                            entryCount++;
                            float progress = (float) entryCount / entrySize;
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
                        File[] fs = dexDir.listFiles();
                        Arrays.sort(fs);
                        List<List<Integer>> itemList = new ArrayList<>();
                        int totalItemCount = 0;
                        boolean isSkip = false;
                        for (int i = 0; i < fs.length; i++) {
                            DexFile dex = new DexFile(fs[i]);
                            List<ClassDefItem> classes = dex.ClassDefsSection.getItems();
                            List<Integer> arrayList = new ArrayList<>();
                            for (int j = 0; j < classes.size(); j++) {
                                float clsProgress = (float) j / fs.length / classes.size() + (float) i / fs.length;
                                activity.runOnUiThread(() -> {
                                    textView.setText("读取类签名");
                                    ViewGroup.LayoutParams lp = progressBackground.getLayoutParams();
                                    lp.height = textView.getHeight();
                                    lp.width = (int) (progressContainer.getWidth() * clsProgress);
                                    progressBackground.setLayoutParams(lp);
                                });
                                String signature = classes.get(j).getClassType().getTypeDescriptor();
                                if (signature.startsWith("Le/b/")) {
                                    arrayList.add(classes.get(j).getIndex());
                                    isSkip = true;
                                } else if (!isSkip && (signature.startsWith("Lcom/baidu/tieba") || signature.startsWith("Lcom/baidu/tbadk")))
                                    arrayList.add(classes.get(j).getIndex());
                            }
                            totalItemCount += arrayList.size();
                            itemList.add(arrayList);
                        }
                        int itemCount = 0;
                        SQLiteDatabase db = activity.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null);
                        for (int i = 0; i < fs.length; i++) {
                            DexFile dex = new DexFile(fs[i]);
                            List<Integer> arrayList = itemList.get(i);
                            for (int j = 0; j < arrayList.size(); j++) {
                                itemCount++;
                                float clsProgress = (float) itemCount / totalItemCount;
                                activity.runOnUiThread(() -> {
                                    textView.setText("搜索");
                                    ViewGroup.LayoutParams lp = progressBackground.getLayoutParams();
                                    lp.height = textView.getHeight();
                                    lp.width = (int) (progressContainer.getWidth() * clsProgress);
                                    progressBackground.setLayoutParams(lp);
                                });
                                ClassDefItem classItem = dex.ClassDefsSection.getItemByIndex(arrayList.get(j));
                                AntiConfusionHelper.searchAndSave(classItem, 0, db);
                                AntiConfusionHelper.searchAndSave(classItem, 1, db);
                            }
                        }
                        SharedPreferences tsConfig = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = tsConfig.edit();
                        byte[] bytes = new byte[32];
                        new FileInputStream(fs[0]).read(bytes);
                        DexFile.calcSignature(bytes);
                        editor.putInt("signature", Arrays.hashCode(bytes));
                        editor.commit();
                        if (Objects.equals(Hook.preferenceMap.get("clean_dir"), true)) {
                            IO.deleteFiles(activity.getExternalCacheDir());
                            IO.deleteFiles(activity.getCacheDir());
                            IO.deleteFiles(new File(activity.getCacheDir().getAbsolutePath() + "image"));
                            IO.deleteFiles(new File(activity.getFilesDir().getAbsolutePath() + File.separator + "newStat" + File.separator + "notUpload"));
                        } else IO.deleteFiles(dexDir);
                        XposedBridge.log("anti-confusion accomplished, current version: " + AntiConfusionHelper.getTbVersion(activity));
                        Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                        }
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