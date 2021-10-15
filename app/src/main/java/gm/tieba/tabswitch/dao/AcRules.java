package gm.tieba.tabswitch.dao;

import android.content.Context;

import androidx.room.Room;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.robv.android.xposed.XposedBridge;

public class AcRules {
    public static final String ACRULES_DATABASE_NAME = "AcRules.db";
    private static AcRuleDatabase sDatabase;
    public static AcRuleDao sDao;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void init(Context context) {
        sDatabase = Room.databaseBuilder(context, AcRuleDatabase.class, ACRULES_DATABASE_NAME)
                .addMigrations(AcRuleMigrations.getMIGRATION_1_2())
                .build();
        sDao = sDatabase.acRuleDao();
    }

    public static void dropRules() {
        executor.submit(() -> sDatabase.acRuleDao().getAll().forEach(it -> sDatabase.acRuleDao().delete(it)));
    }

    public static void putRule(String matcher, String clazz, String method) {
        sDatabase.acRuleDao().insertAll(AcRule.Companion.create(matcher, clazz, method));
    }

    public static Future<?> findRule(Object... rulesAndCallback) {
        return executor.submit(() -> findRuleInternal(rulesAndCallback));
    }

    private static void findRuleInternal(Object... rulesAndCallback) {
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
        var future = executor.submit(() -> !sDao.loadAllMatch(matcher).isEmpty());
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            XposedBridge.log(e);
        }
        return false;
    }

    public static boolean isRuleFound(String... matchers) {
        return Arrays.stream(matchers).allMatch(AcRules::isRuleFound);
    }

    public interface Callback {
        void onRuleFound(String matcher, String clazz, String method);
    }
}
