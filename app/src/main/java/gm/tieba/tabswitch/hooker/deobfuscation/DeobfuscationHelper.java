package gm.tieba.tabswitch.hooker.deobfuscation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.util.DisplayUtils;

public class DeobfuscationHelper {
    private static final int SIGNATURE_DATA_START_OFFSET = 32;
    private static final int SIGNATURE_SIZE = 20;

    static byte[] calcSignature(final InputStream dataStoreInput) throws IOException {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }

        dataStoreInput.skip(SIGNATURE_DATA_START_OFFSET);
        final byte[] buffer = new byte[4 * 1024];
        int bytesRead = dataStoreInput.read(buffer);
        while (bytesRead >= 0) {
            md.update(buffer, 0, bytesRead);
            bytesRead = dataStoreInput.read(buffer);
        }

        final byte[] signature = md.digest();
        if (signature.length != SIGNATURE_SIZE) {
            throw new RuntimeException("unexpected digest write: " + signature.length + " bytes");
        }
        return signature;
    }

    public static boolean isVersionChanged(final Context context) {
        final SharedPreferences tsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
        return !tsConfig.getString("deobfs_version", "unknown").equals(getTbVersion(context));
    }

    public static boolean isDexChanged(final Context context) {
        try {
            final ZipFile zipFile = new ZipFile(new File(context.getPackageResourcePath()));
            try (final InputStream in = zipFile.getInputStream(zipFile.getEntry("classes.dex"))) {
                return Arrays.hashCode(calcSignature(in)) != Preferences.getSignature();
            }
        } catch (final IOException e) {
            XposedBridge.log(e);
        }
        return false;
    }

    public static String getTbVersion(final Context context) {
        final PackageManager pm = context.getPackageManager();
        try {
            final ApplicationInfo applicationInfo = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
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
        } catch (final PackageManager.NameNotFoundException e) {
            XposedBridge.log(e);
            return "unknown";
        }
    }

    @SuppressLint("ApplySharedPref")
    public static void saveAndRestart(final Activity activity, final String version, final Class<?> trampoline) {
        final SharedPreferences.Editor editor = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE).edit();
        editor.putString("deobfs_version", version);
        editor.commit();
        if (trampoline == null) {
            DisplayUtils.restart(activity);
        } else {
            XposedHelpers.findAndHookMethod(trampoline, "onCreate", Bundle.class, new XC_MethodHook() {
                protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                    final Activity activity = (Activity) param.thisObject;
                    DisplayUtils.restart(activity);
                }
            });
            final Intent intent = new Intent(activity, trampoline);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }
    }
}
