package gm.tieba.tabswitch.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class AcRules {
    private static final Map<String, Pair<String, String>> sRulesFromDb = new HashMap<>();

    public static void init(Context context) {
        try (SQLiteDatabase db = context.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null)) {
            try (Cursor c = db.query("rules", null, null, null, null, null, null)) {
                while (c.moveToNext()) {
                    Pair<String, String> pair = new Pair<>(c.getString(2), c.getString(3));
                    sRulesFromDb.put(c.getString(1), pair);
                }
            }
        }
    }

    public static void putRule(SQLiteDatabase db, String rule, String clazz, String method) {
        db.execSQL("insert into rules(rule, class, method) values(?, ?, ?)",
                new Object[]{rule, clazz, method});
    }

    public static void findRule(Object... rulesAndCallback) {
        if (rulesAndCallback.length != 0
                && rulesAndCallback[rulesAndCallback.length - 1] instanceof Callback) {
            Callback callback = (Callback) rulesAndCallback[rulesAndCallback.length - 1];
            for (String rule : getParameterRules(rulesAndCallback)) {
                if (isRuleFound(rule)) {
                    Pair<String, String> pair = sRulesFromDb.get(rule);
                    callback.onRuleFound(rule, pair.first, pair.second);
                }
            }
        } else {
            throw new IllegalArgumentException("no callback defined");
        }
    }

    private static String[] getParameterRules(Object[] rulesAndCallback) {
        if (rulesAndCallback[0] instanceof String[]) return (String[]) rulesAndCallback[0];

        String[] rules = new String[rulesAndCallback.length - 1];
        for (int i = 0; i < rulesAndCallback.length - 1; i++) {
            rules[i] = (String) rulesAndCallback[i];
        }
        return rules;
    }

    public static boolean isRuleFound(String rule) {
        return sRulesFromDb.containsKey(rule);
    }

    public static boolean isRuleFound(String[] rules) {
        if (rules.length != 1) {
            throw new IllegalArgumentException("rules must be a singleton array");
        }
        return isRuleFound(rules[0]);
    }

    public interface Callback {
        void onRuleFound(String rule, String clazz, String method);
    }
}
