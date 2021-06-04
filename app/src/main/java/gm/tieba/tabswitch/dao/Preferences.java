package gm.tieba.tabswitch.dao;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Preferences {
    private static SharedPreferences sTsPreferences;
    private static SharedPreferences sTsConfig;
    private static SharedPreferences sTsNotes;

    public static void init(Context context) {
        //TODO: remove this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File oldSp = new File(context.getDataDir() + File.separator
                    + "shared_prefs" + File.separator + "TS_preference.xml");
            if (oldSp.exists()) {
                SharedPreferences.Editor editor = context.getSharedPreferences(
                        "TS_config", Context.MODE_PRIVATE).edit();
                editor.remove("EULA");
                editor.commit();
                File newSp = new File(context.getDataDir() + File.separator
                        + "shared_prefs" + File.separator + "TS_preferences.xml");
                oldSp.renameTo(newSp);
            }
        }

        sTsPreferences = context.getSharedPreferences("TS_preferences", Context.MODE_PRIVATE);
        sTsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
        sTsNotes = context.getSharedPreferences("TS_notes", Context.MODE_PRIVATE);
    }

    // Preferences
    public static Map<String, ?> getAll() {
        return sTsPreferences.getAll();
    }

    public static void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = sTsPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean getBoolean(String key) {
        return sTsPreferences.getBoolean(key, false);
    }

    public static void putString(String key, String value) {
        SharedPreferences.Editor editor = sTsPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getString(String key) {
        return sTsPreferences.getString(key, null);
    }

    public static void putStringSet(String key, String value, boolean isContain) {
        List<String> list = new ArrayList<>(getStringSet(key));
        if (!isContain) list.remove(value);
        else if (!list.contains(value)) list.add(value);

        SharedPreferences.Editor editor = sTsPreferences.edit();
        editor.putStringSet(key, new HashSet<>(list));
        editor.apply();
    }

    public static Set<String> getStringSet(String key) {
        return sTsPreferences.getStringSet(key, new HashSet<>());
    }

    // Config
    public static void putEULAAccepted() {
        SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putBoolean("EULA", true);
        editor.apply();
    }

    public static boolean getIsEULAAccepted() {
        return sTsConfig.getBoolean("EULA", false);
    }

    public static void putAutoSignEnabled() {
        SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putBoolean("auto_sign", true);
        editor.apply();
    }

    public static boolean getIsAutoSignEnabled() {
        return sTsConfig.getBoolean("auto_sign", false);
    }

    public static void putPurifyEnabled() {
        SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putBoolean("ze", true);
        editor.apply();
    }

    public static boolean getIsPurifyEnabled() {
        return sTsConfig.getBoolean("ze", false);
    }

    @SuppressLint("ApplySharedPref")
    public static void putSignature(int i) {
        SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putInt("signature", i);
        editor.commit();
    }

    public static int getSignature() {
        return sTsConfig.getInt("signature", 0);
    }

    public static void putFollow(List<String> follow) {
        SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putStringSet("follow", new HashSet<>(follow));
        editor.apply();
    }

    public static Set<String> getFollow() {
        return sTsConfig.getStringSet("follow", null);
    }

    public static void putSignDate() {
        SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putInt("sign_date", Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
        editor.apply();
    }

    public static boolean getIsSigned() {
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR) == sTsConfig.getInt("sign_date", 0);
    }

    // Notes
    public static String getNote(String s) {
        return sTsNotes.getString(s, null);
    }

    public static SharedPreferences.Editor getTsNotesEditor() {
        return sTsNotes.edit();
    }
}
