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

public class Preferences {
    private static SharedPreferences sTsPreferences;
    private static SharedPreferences sTsConfig;
    private static SharedPreferences sTsNotes;

    public static void init(final Context context) {
        sTsPreferences = context.getSharedPreferences("TS_preferences", Context.MODE_PRIVATE);
        sTsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
        sTsNotes = context.getSharedPreferences("TS_notes", Context.MODE_PRIVATE);
    }

    // Preferences
    public static Map<String, ?> getAll() {
        return sTsPreferences.getAll();
    }

    public static void remove(final String key) {
        final SharedPreferences.Editor editor = sTsPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    public static void putBoolean(final String key, final boolean value) {
        final SharedPreferences.Editor editor = sTsPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean getBoolean(final String key) {
        return sTsPreferences.getBoolean(key, false);
    }

    public static void putString(final String key, final String value) {
        final SharedPreferences.Editor editor = sTsPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getString(final String key) {
        return sTsPreferences.getString(key, null);
    }

    public static void putStringSet(final String key, final String value, final boolean isContain) {
        final List<String> list = new ArrayList<>(getStringSet(key));
        if (!isContain) list.remove(value);
        else if (!list.contains(value)) list.add(value);

        final SharedPreferences.Editor editor = sTsPreferences.edit();
        editor.putStringSet(key, new HashSet<>(list));
        editor.apply();
    }

    public static Set<String> getStringSet(final String key) {
        return sTsPreferences.getStringSet(key, new HashSet<>());
    }

    // Config
    public static void putEULAAccepted() {
        final SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putBoolean("EULA", true);
        editor.apply();
    }

    public static boolean getIsEULAAccepted() {
        return sTsConfig.getBoolean("EULA", false);
    }

    public static void putAutoSignEnabled() {
        final SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putBoolean("auto_sign", true);
        editor.apply();
    }

    public static boolean getIsAutoSignEnabled() {
        return sTsConfig.getBoolean("auto_sign", false);
    }

    @SuppressLint("ApplySharedPref")
    public static void putPurgeEnabled() {
        final SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putBoolean("ze", true);
        editor.commit();
    }

    public static boolean getIsPurgeEnabled() {
        return sTsConfig.getBoolean("ze", false);
    }

    @SuppressLint("ApplySharedPref")
    public static void putSignature(final int i) {
        final SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putInt("signature", i);
        editor.commit();
    }

    public static int getSignature() {
        return sTsConfig.getInt("signature", 0);
    }

    public static void putLikeForum(final Set<String> follow) {
        final SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putStringSet("like_forum", follow);
        editor.apply();
    }

    public static Set<String> getLikeForum() {
        return sTsConfig.getStringSet("like_forum", null);
    }

    public static void putSignDate() {
        final SharedPreferences.Editor editor = sTsConfig.edit();
        editor.putInt("sign_date", Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
        editor.apply();
    }

    public static boolean getIsSigned() {
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR) == sTsConfig.getInt("sign_date", 0);
    }

    // Notes
    public static Map<String, ?> getNotes() {
        return sTsNotes.getAll();
    }

    public static String getNote(final String name) {
        return sTsNotes.getString(name, null);
    }

    public static SharedPreferences.Editor getTsNotesEditor() {
        return sTsNotes.edit();
    }
}
