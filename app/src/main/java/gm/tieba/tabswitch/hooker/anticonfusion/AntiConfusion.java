package gm.tieba.tabswitch.hooker.anticonfusion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.ClassDefItem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.dao.RulesDbHelper;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.FileUtils;

public class AntiConfusion extends XposedContext implements IHooker {
    private static final String SPRINGBOARD_ACTIVITY = "com.baidu.tieba.tblauncher.MainTabActivity";
    private Activity mActivity;
    private TextView mMessage;
    private TextView mProgress;
    private RelativeLayout mProgressContainer;
    private LinearLayout mContentView;

    public void hook() throws Throwable {
        for (var method : XposedHelpers.findClass("com.baidu.tieba.LogoActivity", sClassLoader).getDeclaredMethods()) {
            if (!method.getName().startsWith("on") && Arrays.equals(method.getParameterTypes(), new Class[]{Bundle.class})) {
                XposedBridge.hookMethod(method, new XC_MethodReplacement() {
                    @SuppressLint("ApplySharedPref")
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        mActivity = (Activity) param.thisObject;
                        if (Preferences.getBoolean("purify")) {
                            var editor = mActivity.getSharedPreferences(
                                    "settings", Context.MODE_PRIVATE).edit();
                            editor.putString("key_location_request_dialog_last_show_version",
                                    AntiConfusionHelper.getTbVersion(mActivity));
                            editor.commit();
                        }

                        if (AntiConfusionHelper.isDexChanged(mActivity)) {
                            mActivity.deleteDatabase("Rules.db");
                        } else if (!AntiConfusionHelper.getRulesLost().isEmpty()) {
                            AntiConfusionHelper.matcherList = AntiConfusionHelper.getRulesLost();
                        } else {
                            AntiConfusionHelper.saveAndRestart(mActivity, AntiConfusionHelper.getTbVersion(mActivity),
                                    XposedHelpers.findClass(SPRINGBOARD_ACTIVITY, sClassLoader));
                        }

                        initProgressIndicator();
                        mActivity.setContentView(mContentView);
                        new Thread(() -> {
                            var dexDir = new File(mActivity.getCacheDir(), "app_dex");
                            try {
                                FileUtils.deleteRecursively(dexDir);
                                dexDir.mkdirs();
                                var zipFile = new ZipFile(new File(mActivity.getPackageResourcePath()));
                                var enumeration = zipFile.entries();
                                var entryCount = 0;
                                var entrySize = zipFile.size();
                                setMessage("解压");
                                while (enumeration.hasMoreElements()) {
                                    entryCount++;
                                    setProgress((float) entryCount / entrySize);

                                    var ze = enumeration.nextElement();
                                    if (ze.getName().matches("classes[0-9]*?\\.dex")) {
                                        FileUtils.copy(zipFile.getInputStream(ze), new File(dexDir, ze.getName()));
                                    }
                                }
                                var fs = dexDir.listFiles();
                                if (fs == null) throw new FileNotFoundException("解压失败");
                                Arrays.sort(fs, (o1, o2) -> {
                                    var ints = new int[2];
                                    var comparingFiles = new File[]{o1, o2};
                                    for (var i = 0; i < comparingFiles.length; i++) {
                                        try {
                                            ints[i] = Integer.parseInt(comparingFiles[i].getName().replaceAll("[a-z.]", ""));
                                        } catch (NumberFormatException e) {
                                            ints[i] = 1;
                                        }
                                    }
                                    return ints[0] - ints[1];
                                });
                                // special optimization for TbDialog
                                var dialogMatcher = "\"Dialog must be created by function create()!\"";
                                var dialogClasses = new HashSet<String>();
                                AntiConfusionHelper.matcherList.add(dialogMatcher);
                                var searcher = new DexBakSearcher(AntiConfusionHelper.matcherList);
                                try (var db = new RulesDbHelper(mActivity).getReadableDatabase()) {
                                    var progress = 0f;
                                    setMessage("搜索字符串");
                                    for (var f : fs) {
                                        try (var in = new BufferedInputStream(new FileInputStream(f))) {
                                            var dex = DexBackedDexFile.fromInputStream(null, in);
                                            var classDefs = new ArrayList<>(dex.getClasses());
                                            for (var i = 0; i < classDefs.size(); i++) {
                                                progress += (float) 1 / fs.length / classDefs.size();
                                                setProgress(progress);

                                                searcher.searchString(classDefs.get(i), (matcher, clazz, method1) -> {
                                                    if (matcher.equals(dialogMatcher)) {
                                                        dialogClasses.add(searcher.revert(clazz));
                                                    } else {
                                                        AcRules.putRule(db, matcher, clazz, method1);
                                                    }
                                                });
                                            }
                                        }
                                    }
                                    var stringClasses = new ArrayList<String>();
                                    try (var c = db.query("rules", null, null, null, null, null, null)) {
                                        while (c.moveToNext()) {
                                            stringClasses.add(c.getString(2));
                                        }
                                    }
                                    var first = new ArrayList<String>();
                                    var second = new ArrayList<String>();
                                    var third = new ArrayList<String>();
                                    stringClasses.forEach(s -> {
                                        var split = s.split("\\.");
                                        first.add(split[0]);
                                        second.add(split[1]);
                                        third.add(split[2]);
                                    });
                                    var most = "L" + searcher.most(first) + "/" + searcher.most(second) + "/" + searcher.most(third) + "/";
                                    var numberOfClassesToSearch = new int[fs.length];
                                    var totalClassesToSearch = 0;
                                    for (var i = 0; i < fs.length; i++) {
                                        try (var in = new BufferedInputStream(new FileInputStream(fs[i]))) {
                                            var dex = DexBackedDexFile.fromInputStream(null, in);
                                            var classDefs = ClassDefItem.getClasses(dex);
                                            var count = 0;
                                            for (var classDef : classDefs) {
                                                if (classDef.startsWith(most) || dialogClasses.contains(classDef)) {
                                                    count++;
                                                }
                                            }
                                            numberOfClassesToSearch[i] = count;
                                            totalClassesToSearch += count;
                                        }
                                    }
                                    var searchedClassCount = 0;
                                    setMessage("在 " + most + " 中搜索代码");
                                    for (var i = 0; i < fs.length; i++) {
                                        if (numberOfClassesToSearch[i] == 0) {
                                            continue;
                                        }
                                        try (var in = new BufferedInputStream(new FileInputStream(fs[i]))) {
                                            var dex = DexBackedDexFile.fromInputStream(null, in);
                                            var classDefs = new ArrayList<>(dex.getClasses());
                                            for (var classDef : classDefs) {
                                                var signature = classDef.getType();
                                                if (signature.startsWith(most) || dialogClasses.contains(signature)) {
                                                    searchedClassCount++;
                                                    setProgress((float) searchedClassCount / totalClassesToSearch);

                                                    searcher.searchSmali(classDef, (matcher, clazz, method2) ->
                                                            AcRules.putRule(db, matcher, clazz, method2));
                                                }
                                            }
                                        }
                                    }
                                }
                                try (var in = new FileInputStream(fs[0])) {
                                    Preferences.putSignature(Arrays.hashCode(AntiConfusionHelper.calcSignature(in)));
                                }
                                XposedBridge.log("anti-confusion accomplished, current version: "
                                        + AntiConfusionHelper.getTbVersion(mActivity));
                                AntiConfusionHelper.saveAndRestart(mActivity, AntiConfusionHelper.getTbVersion(mActivity),
                                        XposedHelpers.findClass(SPRINGBOARD_ACTIVITY, sClassLoader));
                            } catch (Throwable e) {
                                XposedBridge.log(e);
                                setMessage("处理失败\n" + Log.getStackTraceString(e));
                            }
                        }).start();
                        return null;
                    }
                });
            }
        }
    }

    @SuppressLint({"SetTextI18n"})
    private void initProgressIndicator() {
        var title = new TextView(mActivity);
        title.setTextSize(16);
        title.setPadding(0, 0, 0, 20);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.parseColor("#FF303030"));
        title.setText("贴吧TS正在定位被混淆的类和方法，请耐心等待");
        mMessage = new TextView(mActivity);
        mMessage.setTextSize(16);
        mMessage.setTextColor(Color.parseColor("#FF303030"));
        mProgress = new TextView(mActivity);
        mProgress.setBackgroundColor(Color.parseColor("#FFBEBEBE"));
        mProgressContainer = new RelativeLayout(mActivity);
        mProgressContainer.addView(mProgress);
        mProgressContainer.addView(mMessage);
        var tvLp = (RelativeLayout.LayoutParams) mMessage.getLayoutParams();
        tvLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mMessage.setLayoutParams(tvLp);
        var rlLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mProgressContainer.setLayoutParams(rlLp);
        mContentView = new LinearLayout(mActivity);
        mContentView.setOrientation(LinearLayout.VERTICAL);
        mContentView.setGravity(Gravity.CENTER);
        mContentView.addView(title);
        mContentView.addView(mProgressContainer);
    }

    private void setMessage(String message) {
        mActivity.runOnUiThread(() -> mMessage.setText(message));
    }

    private void setProgress(float progress) {
        mActivity.runOnUiThread(() -> {
            var lp = mProgress.getLayoutParams();
            lp.height = mMessage.getHeight();
            lp.width = Math.round(mProgressContainer.getWidth() * progress);
            mProgress.setLayoutParams(lp);
        });
    }
}
