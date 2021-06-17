package gm.tieba.tabswitch.hooker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.dao.RulesDbHelper;
import gm.tieba.tabswitch.util.IO;

public class AntiConfusion extends BaseHooker implements IHooker {
    private static final String SPRINGBOARD_ACTIVITY = "com.baidu.tieba.tblauncher.MainTabActivity";
    private Activity mActivity;
    private TextView mMessage;
    private TextView mProgress;
    private RelativeLayout mProgressContainer;
    private LinearLayout mContentView;

    public AntiConfusion(ClassLoader classLoader, Context context, Resources res) {
        super(classLoader, context, res);
    }

    public void hook() throws Throwable {
        for (Method method : sClassLoader.loadClass("com.baidu.tieba.LogoActivity").getDeclaredMethods()) {
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
                            AntiConfusionHelper.saveAndRestart(mActivity, AntiConfusionHelper
                                    .getTbVersion(mActivity), sClassLoader.loadClass(SPRINGBOARD_ACTIVITY), sRes);
                        }
                        new RulesDbHelper(mActivity.getApplicationContext()).getReadableDatabase();

                        initProgressIndicator();
                        mActivity.setContentView(mContentView);
                        new Thread(() -> {
                            File dexDir = new File(mActivity.getCacheDir(), "app_dex");
                            try {
                                IO.deleteRecursively(dexDir);
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
                                        IO.copy(zipFile.getInputStream(ze), new File(dexDir, ze.getName()));
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
                                List<List<Integer>> totalIndexes = new ArrayList<>();
                                int totalItemCount = 0;
                                boolean isSkip = false;
                                for (int i = 0; i < fs.length; i++) {
                                    DexFile dex = new DexFile(fs[i]);
                                    List<ClassDefItem> classes = dex.ClassDefsSection.getItems();
                                    List<Integer> indexes = new ArrayList<>();
                                    for (int j = 0; j < classes.size(); j++) {
                                        setProgress("读取类签名", (float)
                                                j / fs.length / classes.size() + (float) i / fs.length);

                                        String signature = classes.get(j).getClassType().getTypeDescriptor();
                                        if (signature.matches("Ld/[a-b]/.*")) {
                                            indexes.add(classes.get(j).getIndex());
                                            isSkip = true;
                                        } else if (signature.startsWith("Lcom/baidu/tbadk")
                                                || !isSkip && (signature.startsWith("Lcom/baidu/tieba"))) {
                                            indexes.add(classes.get(j).getIndex());
                                        }
                                    }
                                    totalItemCount += indexes.size();
                                    totalIndexes.add(indexes);
                                }
                                int itemCount = 0;
                                SQLiteDatabase db = mActivity.openOrCreateDatabase("Rules.db",
                                        Context.MODE_PRIVATE, null);
                                for (int i = 0; i < fs.length; i++) {
                                    DexFile dex = new DexFile(fs[i]);
                                    List<Integer> indexes = totalIndexes.get(i);
                                    for (int j = 0; j < indexes.size(); j++) {
                                        itemCount++;
                                        setProgress("搜索", (float) itemCount / totalItemCount);

                                        ClassDefItem classItem = dex.ClassDefsSection
                                                .getItemByIndex(indexes.get(j));
                                        AntiConfusionHelper.searchAndSave(classItem, 0, db);
                                        AntiConfusionHelper.searchAndSave(classItem, 1, db);
                                    }
                                }
                                mActivity.runOnUiThread(() -> mMessage.setText("保存反混淆信息"));
                                byte[] bytes = new byte[32];
                                new FileInputStream(fs[0]).read(bytes);
                                DexFile.calcSignature(bytes);
                                Preferences.putSignature(Arrays.hashCode(bytes));
                                XposedBridge.log("anti-confusion accomplished, current version: "
                                        + AntiConfusionHelper.getTbVersion(mActivity));
                                AntiConfusionHelper.saveAndRestart(mActivity, AntiConfusionHelper
                                        .getTbVersion(mActivity), sClassLoader.loadClass(SPRINGBOARD_ACTIVITY), sRes);
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
        title.setTextColor(sRes.getColor(R.color.colorPrimaryDark, null));
        title.setText("贴吧TS正在定位被混淆的类和方法，请耐心等待");
        mMessage = new TextView(mActivity);
        mMessage.setTextSize(16);
        mMessage.setTextColor(sRes.getColor(R.color.colorPrimaryDark, null));
        mMessage.setText("读取ZipEntry");
        mProgress = new TextView(mActivity);
        mProgress.setBackgroundColor(sRes.getColor(R.color.colorProgress, null));
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
            lp.width = (int) (mProgressContainer.getWidth() * progress);
            mProgress.setLayoutParams(lp);
        });
    }
}
