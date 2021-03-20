package gm.tieba.tabswitch.hookImpl;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import bin.zip.ZipEntry;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.RulesDbHelper;
import gm.tieba.tabswitch.util.IO;

public class AntiConfusion extends Hook {
    private static final String springboardActivity = "com.baidu.tieba.tblauncher.MainTabActivity";

    public static void hook(ClassLoader classLoader) throws Throwable {
        Method[] methods = classLoader.loadClass("com.baidu.tieba.LogoActivity").getDeclaredMethods();
        for (Method method : methods)
            if (Arrays.toString(method.getParameterTypes()).equals("[class android.os.Bundle]") && !method.getName().startsWith("on"))
                XposedBridge.hookMethod(method, new XC_MethodReplacement() {
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        if (AntiConfusionHelper.isDexChanged(activity))
                            activity.deleteDatabase("Rules.db");
                        else if (AntiConfusionHelper.getLostList().size() == 0)
                            AntiConfusionHelper.saveAndRestart(activity, AntiConfusionHelper.getTbVersion(activity), classLoader.loadClass(springboardActivity));
                        else AntiConfusionHelper.matcherList = AntiConfusionHelper.getLostList();
                        new RulesDbHelper(activity.getApplicationContext()).getReadableDatabase();

                        TextView title = new TextView(activity);
                        title.setTextSize(16);
                        title.setPadding(0, 0, 0, 20);
                        title.setGravity(Gravity.CENTER);
                        title.setTextColor(Hook.modRes.getColor(R.color.colorPrimaryDark, null));
                        title.setText("贴吧TS正在定位被混淆的类和方法，请耐心等待");
                        TextView textView = new TextView(activity);
                        textView.setTextSize(16);
                        textView.setTextColor(Hook.modRes.getColor(R.color.colorPrimaryDark, null));
                        textView.setText("读取ZipEntry");
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
                        LinearLayout linearLayout = new LinearLayout(activity);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);
                        linearLayout.setGravity(Gravity.CENTER);
                        linearLayout.addView(title);
                        linearLayout.addView(progressContainer);
                        activity.setContentView(linearLayout);
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
                                        if (signature.startsWith("Ld/b/") || signature.startsWith("Le/b/")) {
                                            arrayList.add(classes.get(j).getIndex());
                                            isSkip = true;
                                        } else if (signature.startsWith("Lcom/baidu/tbadk") || !isSkip && (signature.startsWith("Lcom/baidu/tieba")))
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
                                activity.runOnUiThread(() -> textView.setText("保存反混淆信息"));
                                SharedPreferences tsConfig = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = tsConfig.edit();
                                byte[] bytes = new byte[32];
                                new FileInputStream(fs[0]).read(bytes);
                                DexFile.calcSignature(bytes);
                                editor.putInt("signature", Arrays.hashCode(bytes));
                                editor.apply();
                                SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
                                if (tsPreference.getBoolean("clean_dir", false)) {
                                    IO.deleteFiles(activity.getExternalCacheDir());
                                    IO.deleteFiles(activity.getCacheDir());
                                    IO.deleteFiles(new File(activity.getCacheDir().getAbsolutePath() + "image"));
                                    IO.deleteFiles(new File(activity.getFilesDir().getAbsolutePath() + File.separator + "newStat" + File.separator + "notUpload"));
                                } else IO.deleteFiles(dexDir);
                                XposedBridge.log("anti-confusion accomplished, current version: " + AntiConfusionHelper.getTbVersion(activity));
                                AntiConfusionHelper.saveAndRestart(activity, AntiConfusionHelper.getTbVersion(activity), classLoader.loadClass(springboardActivity));
                            } catch (Throwable throwable) {
                                activity.runOnUiThread(() -> textView.setText(String.format("处理失败\n%s", Log.getStackTraceString(throwable))));
                                XposedBridge.log(throwable);
                            }
                        }).start();
                        return null;
                    }
                });
    }
}