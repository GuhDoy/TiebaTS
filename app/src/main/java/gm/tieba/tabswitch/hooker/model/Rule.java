package gm.tieba.tabswitch.hooker.model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Rule {
    private static List<Map<String, String>> sRulesFromDb;

    public static void init(Context context) {
        sRulesFromDb = new ArrayList<>();
        SQLiteDatabase db = context.openOrCreateDatabase("Rules.db", Context.MODE_PRIVATE, null);
        Cursor c = db.query("rules", null, null, null, null, null, null);
        for (int j = 0; j < c.getCount(); j++) {
            c.moveToNext();
            Map<String, String> map = new HashMap<>();
            map.put("rule", c.getString(1));
            map.put("class", c.getString(2));
            map.put("method", c.getString(3));
            sRulesFromDb.add(map);
        }
        c.close();
        db.close();
    }

    public interface RuleCallBack {
        void onRuleFound(String rule, String clazz, String method) throws Throwable;
    }

    public static void findRule(RuleCallBack ruleCallBack, String... rules) throws Throwable {
        for (String rule : rules) {
            for (Map<String, String> map : sRulesFromDb) {
                if (Objects.equals(map.get("rule"), rule)) {
                    ruleCallBack.onRuleFound(rule, map.get("class"), map.get("method"));
                }
            }
        }
    }

    public static boolean isRuleFound(String rule) throws Throwable {
        for (Map<String, String> map : sRulesFromDb) {
            if (Objects.equals(map.get("rule"), rule)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> getRulesFound() {
        List<String> ruleList = new ArrayList<>();
        for (int i = 0; i < sRulesFromDb.size(); i++) {
            Map<String, String> map = sRulesFromDb.get(i);
            ruleList.add(map.get("rule"));
        }
        return ruleList;
    }
}