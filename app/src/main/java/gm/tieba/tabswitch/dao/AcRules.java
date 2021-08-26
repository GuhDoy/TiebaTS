package gm.tieba.tabswitch.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AcRules {
    private static final Map<String, Pair<String, String>> sRulesFromDb = new HashMap<>();

    public static void init(Context context) {
        try (var db = new RulesDbHelper(context).getReadableDatabase()) {
            try (var c = db.query("rules", null, null, null, null, null, null)) {
                while (c.moveToNext()) {
                    var pair = new Pair<>(c.getString(2), c.getString(3));
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
            var callback = (Callback) rulesAndCallback[rulesAndCallback.length - 1];
            for (var rule : getParameterRules(rulesAndCallback)) {
                if (isRuleFound(rule)) {
                    var pair = sRulesFromDb.get(rule);
                    callback.onRuleFound(rule, pair.first, pair.second);
                }
            }
        } else {
            throw new IllegalArgumentException("no callback defined");
        }
    }

    private static String[] getParameterRules(Object[] rulesAndCallback) {
        if (rulesAndCallback[0] instanceof String[]) {
            return (String[]) rulesAndCallback[0];
        }
        var rules = new String[rulesAndCallback.length - 1];
        for (var i = 0; i < rulesAndCallback.length - 1; i++) {
            rules[i] = (String) rulesAndCallback[i];
        }
        return rules;
    }

    public static boolean isRuleFound(String rule) {
        return sRulesFromDb.containsKey(rule);
    }

    public static boolean isRuleFound(String... rules) {
        return Arrays.stream(rules).allMatch(AcRules::isRuleFound);
    }

    public interface Callback {
        void onRuleFound(String rule, String clazz, String method);
    }
}
