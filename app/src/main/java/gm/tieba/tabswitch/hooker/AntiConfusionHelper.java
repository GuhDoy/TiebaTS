package gm.tieba.tabswitch.hooker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.util.IndentingWriter2;
import org.jf.util.Parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.util.DisplayUtils;

public class AntiConfusionHelper {
    static List<String> matcherList = new ArrayList<>();

    static {
        Constants.getMatchers().values().forEach(strings -> matcherList.addAll(Arrays.asList(strings)));
    }

    public static List<String> getRulesLost() {
        List<String> list = new ArrayList<>(matcherList);
        list.removeIf(AcRules::isRuleFound);
        return list;
    }

    public static boolean isVersionChanged(Context context) {
        SharedPreferences tsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
        return !tsConfig.getString("anti-confusion_version", "unknown").equals(getTbVersion(context));
    }

    public static boolean isDexChanged(Context context) {
        try {
            ZipFile zipFile = new ZipFile(new File(context.getPackageResourcePath()));
            byte[] bytes = new byte[32];
            zipFile.getInputStream(zipFile.getEntry("classes.dex")).read(bytes);
            DexFile.calcSignature(bytes);
            return Arrays.hashCode(bytes) != Preferences.getSignature();
        } catch (IOException e) {
            XposedBridge.log(e);
        }
        return false;
    }

    @SuppressLint("ApplySharedPref")
    public static void saveAndRestart(Activity activity, String value, Class<?> springboardActivity) {
        SharedPreferences.Editor editor = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE).edit();
        editor.putString("anti-confusion_version", value);
        editor.commit();
        if (springboardActivity == null) DisplayUtils.restart(activity);
        else {
            XposedHelpers.findAndHookMethod(springboardActivity, "onCreate", Bundle.class, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    DisplayUtils.restart(activity);
                }
            });
            Intent intent = new Intent(activity, springboardActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }
    }

    static void searchAndSave(ClassDefItem classItem, int type, SQLiteDatabase db) throws IOException {
        ClassDataItem.EncodedMethod[] methods = null;
        try {
            if (type == 0) methods = classItem.getClassData().getDirectMethods();
            else if (type == 1) methods = classItem.getClassData().getVirtualMethods();
        } catch (NullPointerException e) {
            return;
        }
        for (ClassDataItem.EncodedMethod method : methods) {
            Parser parser = new Parser(method.codeItem);
            IndentingWriter2 writer = new IndentingWriter2();
            parser.dump(writer);
            for (int i = 0; i < matcherList.size(); i++) {
                if (writer.getString().contains(matcherList.get(i))) {
                    String clazz = classItem.getClassType().getTypeDescriptor();
                    clazz = clazz.substring(clazz.indexOf("L") + 1, clazz.indexOf(";")).replace("/", ".");
                    db.execSQL("insert into rules(rule, class, method) values(?, ?, ?)",
                            new Object[]{matcherList.get(i), clazz, method.method.methodName.getStringValue()});
                    return;
                }
            }
        }
    }

    public static String getTbVersion(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            switch ((Integer) applicationInfo.metaData.get("versionType")) {
                case 3:
                    return pm.getPackageInfo(context.getPackageName(), 0).versionName;
                case 2:
                    return String.valueOf(applicationInfo.metaData.get("grayVersion"));
                case 1:
                    return String.valueOf(applicationInfo.metaData.get("subVersion"));
                default:
                    throw new PackageManager.NameNotFoundException("unknown tb version");
            }
        } catch (PackageManager.NameNotFoundException e) {
            XposedBridge.log(e);
            return "unknown";
        }
    }
}
