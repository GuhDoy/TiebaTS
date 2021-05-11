package gm.tieba.tabswitch.dao;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressLint("ApplySharedPref")
public class Preferences {
    private static SharedPreferences sTsPreferences;
    private static SharedPreferences sTsConfig;
    private static SharedPreferences sTsNotes;

    public static void init(Context context) {
        sTsPreferences = context.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
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
        editor.commit();
    }

    public static boolean getBoolean(String s) {
        return sTsPreferences.getBoolean(s, false);
    }

    public static void putString(String key, String value) {
        SharedPreferences.Editor editor = sTsPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getString(String s) {
        return sTsPreferences.getString(s, null);
    }

    public static void putStringSet(String key, boolean value) {
        List<String> list = new ArrayList<>(getStringSet());
        if (!value) {
            list.remove(key);
        } else if (!list.contains(key)) {
            list.add(key);
        }
        SharedPreferences.Editor editor = sTsPreferences.edit();
        editor.putStringSet("switch_manager", new HashSet<>(list));
        editor.commit();
    }

    public static Set<String> getStringSet() {
        return sTsPreferences.getStringSet("switch_manager", new HashSet<>());
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
