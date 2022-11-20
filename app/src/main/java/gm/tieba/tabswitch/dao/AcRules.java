package gm.tieba.tabswitch.dao;

import android.content.Context;

import androidx.room.Room;

import java.util.Arrays;

public class AcRules {
    public static final String ACRULES_DATABASE_NAME = "AcRules.db";
    public static AcRuleDao sDao;

    public static void init(Context context) {
        sDao = Room.databaseBuilder(
                        context.getApplicationContext(), AcRuleDatabase.class, ACRULES_DATABASE_NAME
                )
                .allowMainThreadQueries()
                .build()
                .acRuleDao();
    }

    public static void dropRules() {
        sDao.getAll().forEach(it -> sDao.delete(it));
    }

    public static void putRule(String matcher, String clazz, String method) {
        sDao.insertAll(AcRule.Companion.create(matcher, clazz, method));
    }

    public static void findRule(Object... rulesAndCallback) {
        if (rulesAndCallback.length != 0
                && rulesAndCallback[rulesAndCallback.length - 1] instanceof Callback) {
            var callback = (Callback) rulesAndCallback[rulesAndCallback.length - 1];
            for (var rule : sDao.loadAllMatch(getParameterRules(rulesAndCallback))) {
                callback.onRuleFound(rule.getMatcher(), rule.getClazz(), rule.getMethod());
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

    public static boolean isRuleFound(String matcher) {
        return !sDao.loadAllMatch(matcher).isEmpty();
    }

    public static boolean isRuleFound(String... matchers) {
        return Arrays.stream(matchers).allMatch(AcRules::isRuleFound);
    }

    public interface Callback {
        void onRuleFound(String matcher, String clazz, String method);
    }
}
