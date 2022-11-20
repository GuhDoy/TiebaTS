package gm.tieba.tabswitch.dao;

import android.content.Context;

import androidx.room.Room;

import java.util.Arrays;
import java.util.List;

import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;

public class AcRules {
    public static final String ACRULES_DATABASE_NAME = "Deobfs.db";
    public static AcRuleDao sDao;

    public static void init(Context context) {
        sDao = Room.databaseBuilder(
                        context.getApplicationContext(), AcRuleDatabase.class, ACRULES_DATABASE_NAME
                )
                .allowMainThreadQueries()
                .build()
                .acRuleDao();
    }

    public static void dropAllRules() {
        sDao.getAll().forEach(it -> sDao.delete(it));
    }

    public static void putRule(String matcher, String clazz, String method) {
        sDao.insertAll(AcRule.Companion.create(matcher, clazz, method));
    }

    public static void findRule(Matcher matcher, Callback callback) {
        for (var rule : sDao.loadAllMatch(matcher.toString())) {
            callback.onRuleFound(rule.getMatcher(), rule.getClazz(), rule.getMethod());
        }
    }

    public static void findRule(List<? extends Matcher> matchers, Callback callback) {
        for (var rule : sDao.loadAllMatch(matchers.stream().map(Matcher::toString).toArray(String[]::new))) {
            callback.onRuleFound(rule.getMatcher(), rule.getClazz(), rule.getMethod());
        }
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
