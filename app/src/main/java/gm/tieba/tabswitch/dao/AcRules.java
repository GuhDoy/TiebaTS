package gm.tieba.tabswitch.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AcRules {
    private static List<Map<String, String>> sRulesFromDb;

    public static void init(Context context) {
        sRulesFromDb = new ArrayList<>();
        try (SQLiteDatabase db = context.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null)) {
            try (Cursor c = db.query("rules", null, null, null, null, null, null)) {
                for (int i = 0; i < c.getCount(); i++) {
                    c.moveToNext();
                    Map<String, String> map = new ArrayMap<>();
                    map.put("rule", c.getString(1));
                    map.put("class", c.getString(2));
                    map.put("method", c.getString(3));
                    sRulesFromDb.add(map);
                }
            }
        }
    }

    public static void findRule(Object... rulesAndCallback) {
        if (rulesAndCallback.length != 0
                && rulesAndCallback[rulesAndCallback.length - 1] instanceof Callback) {
            Callback callback = (Callback) rulesAndCallback[rulesAndCallback.length - 1];
            for (String rule : getParameterRules(rulesAndCallback)) {
                for (Map<String, String> map : sRulesFromDb) {
                    if (Objects.equals(map.get("rule"), rule)) {
                        callback.onRuleFound(rule, map.get("class"), map.get("method"));
                    }
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
        for (Map<String, String> map : sRulesFromDb) {
            if (Objects.equals(map.get("rule"), rule)) {
                return true;
            }
        }
        return false;
    }

    public interface Callback {
        void onRuleFound(String rule, String clazz, String method);
    }
}
