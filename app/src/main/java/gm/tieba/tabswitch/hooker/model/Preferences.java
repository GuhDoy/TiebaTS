package gm.tieba.tabswitch.hooker.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

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

    public static boolean getIsCleanDir() {
        return sTsPreferences.getBoolean("clean_dir", false);
    }

    public static boolean getIsPurify() {
        return sTsPreferences.getBoolean("purify", false);
    }

    public static String getPersonalizedFilter() {
        return sTsPreferences.getString("personalized_filter", null);
    }

    public static String getContentFilter() {
        return sTsPreferences.getString("content_filter", null);
    }

    public static Map<String, ?> getAll() {
        return sTsPreferences.getAll();
    }

    public static boolean getBoolean(String s) {
        return sTsPreferences.getBoolean(s, false);
    }

    public static String getString(String s) {
        return sTsPreferences.getString(s, null);
    }

    public static SharedPreferences.Editor getTsPreferencesEditor() {
        return sTsPreferences.edit();
    }

    public static void setEULAAccepted() {
        SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putBoolean("EULA", true);
        editor.apply();
    }

    public static boolean getIsEULAAccepted() {
        return sTsConfig.getBoolean("EULA", false);
    }

    public static void setAutoSignEnabled() {
        SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putBoolean("auto_sign", true);
        editor.apply();
    }

    public static boolean getIsAutoSignEnabled() {
        return sTsConfig.getBoolean("auto_sign", false);
    }

    public static void setPurifyEnabled() {
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

    public static boolean getIsPurifyEnter() {
        return sTsPreferences.getBoolean("purify_enter", false);
    }

    public static String getNote(String s) {
        return sTsNotes.getString(s, null);
    }

    public static SharedPreferences.Editor getTsNotesEditor() {
        return sTsNotes.edit();
    }
}