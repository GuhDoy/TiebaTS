package gm.tieba.tabswitch.hooker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.dao.RulesDbHelper;
import gm.tieba.tabswitch.util.FileUtils;

public class AntiConfusion extends XposedContext implements IHooker {
    private static final String SPRINGBOARD_ACTIVITY = "com.baidu.tieba.tblauncher.MainTabActivity";
    private Activity mActivity;
    private TextView mMessage;
    private TextView mProgress;
    private RelativeLayout mProgressContainer;
    private LinearLayout mContentView;

    public void hook() throws Throwable {
        for (Method method : XposedHelpers.findClass("com.baidu.tieba.LogoActivity", sClassLoader).getDeclaredMethods()) {
            if (!method.getName().startsWith("on") && Arrays.toString(method.getParameterTypes())
                    .equals("[class android.os.Bundle]")) {
                XposedBridge.hookMethod(method, new XC_MethodReplacement() {
                    @SuppressLint("ApplySharedPref")
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        mActivity = (Activity) param.thisObject;
                        if (Preferences.getBoolean("purify")) {
                            SharedPreferences.Editor editor = mActivity.getSharedPreferences(
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
                            File dexDir = new File(mActivity.getCacheDir(), "app_dex");
                            try {
                                FileUtils.deleteRecursively(dexDir);
                                dexDir.mkdirs();
                                ZipFile zipFile = new ZipFile(new File(mActivity.getPackageResourcePath()));
                                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                                int entryCount = 0;
                                int entrySize = zipFile.size();
                                while (enumeration.hasMoreElements()) {
                                    entryCount++;
                                    setProgress("解压", (float) entryCount / entrySize);

                                    ZipEntry ze = enumeration.nextElement();
                                    if (ze.getName().matches("classes[0-9]*?\\.dex")) {
                                        FileUtils.copy(zipFile.getInputStream(ze), new File(dexDir, ze.getName()));
                                    }
                                }
                                File[] fs = dexDir.listFiles();
                                if (fs == null) throw new FileNotFoundException("解压失败");
                                Arrays.sort(fs, (o1, o2) -> {
                                    int i1, i2;
                                    try {
                                        i1 = Integer.parseInt(o1.getName().replaceAll("[a-z.]", ""));
                                    } catch (NumberFormatException e) {
                                        i1 = 1;
                                    }
                                    try {
                                        i2 = Integer.parseInt(o2.getName().replaceAll("[a-z.]", ""));
                                    } catch (NumberFormatException e) {
                                        i2 = 1;
                                    }
                                    return i1 - i2;
                                });
                                float progress = 0;
                                try (SQLiteDatabase db = new RulesDbHelper(mActivity).getReadableDatabase()) {
                                    for (File f : fs) {
                                        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
                                            DexBackedDexFile dex = DexBackedDexFile.fromInputStream(null, in);
                                            List<? extends ClassDef> classDefs = new ArrayList<>(dex.getClasses());
                                            for (int i = 0; i < classDefs.size(); i++) {
                                                progress += (float) 1 / fs.length / classDefs.size();
                                                setProgress("搜索", progress);

                                                String signature = classDefs.get(i).getType();
                                                if (signature.startsWith("Lc/a/")
                                                        || signature.startsWith("Lc/b/")
                                                        || signature.startsWith("Lcom/baidu/tieba/")
                                                        || signature.startsWith("Lcom/baidu/tbadk/")) {

                                                    AntiConfusionHelper.searchAndSave(classDefs.get(i), db);
                                                }
                                            }
                                        }
                                    }
                                }
                                try (InputStream in = new FileInputStream(fs[0])) {
                                    Preferences.putSignature(Arrays.hashCode(AntiConfusionHelper.calcSignature(in)));
                                }
                                XposedBridge.log("anti-confusion accomplished, current version: "
                                        + AntiConfusionHelper.getTbVersion(mActivity));
                                AntiConfusionHelper.saveAndRestart(mActivity, AntiConfusionHelper.getTbVersion(mActivity),
                                        XposedHelpers.findClass(SPRINGBOARD_ACTIVITY, sClassLoader));
                            } catch (Throwable e) {
                                XposedBridge.log(e);
                                mActivity.runOnUiThread(() -> mMessage.setText(String.format(
                                        "处理失败\n%s", Log.getStackTraceString(e))));
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
        TextView title = new TextView(mActivity);
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
        RelativeLayout.LayoutParams tvLp = (RelativeLayout.LayoutParams) mMessage.getLayoutParams();
        tvLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mMessage.setLayoutParams(tvLp);
        RelativeLayout.LayoutParams rlLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mProgressContainer.setLayoutParams(rlLp);
        mContentView = new LinearLayout(mActivity);
        mContentView.setOrientation(LinearLayout.VERTICAL);
        mContentView.setGravity(Gravity.CENTER);
        mContentView.addView(title);
        mContentView.addView(mProgressContainer);
    }

    private void setProgress(String message, float progress) {
        mActivity.runOnUiThread(() -> {
            mMessage.setText(message);
            ViewGroup.LayoutParams lp = mProgress.getLayoutParams();
            lp.height = mMessage.getHeight();
            lp.width = Math.round(mProgressContainer.getWidth() * progress);
            mProgress.setLayoutParams(lp);
        });
    }
}
