package gm.tieba.tabswitch.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;

public class Adp extends XposedContext {
    private static Adp sAdp;
    public String BDUSS;
    public String tbs;
    public String account;
    public Set<String> follows = new HashSet<>();
    private SQLiteDatabase mDb;

    public Adp() {
        sAdp = this;
        getAccountData();
        refreshCache();
    }

    private void getAccountData() {
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.data.AccountData", sClassLoader,
                "getBDUSS", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        BDUSS = (String) param.getResult();
                    }
                });
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.data.AccountData", sClassLoader,
                "getTbs", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        tbs = (String) param.getResult();
                    }
                });
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.data.AccountData", sClassLoader,
                "getAccount", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        account = (String) param.getResult();
                    }
                });
    }

    private void refreshCache() {
        XposedHelpers.findAndHookMethod("tbclient.ForumRecommend.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            public void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final Set<String> forums = new HashSet<>();
                final List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "like_forum");
                if (list == null) return;
                for (int i = 0; i < list.size(); i++) {
                    forums.add((String) XposedHelpers.getObjectField(list.get(i), "forum_name"));
                }
                Preferences.putLikeForum(forums);
            }
        });
    }

    // lazy init
    public static Adp getInstance() {
        return sAdp;
    }

    public synchronized Adp parseDatabase() throws JSONException {
        String myPagesTable = null;
        mDb = getContext().openOrCreateDatabase("baidu_adp.db", Context.MODE_PRIVATE, null);
        try (final Cursor c = mDb.query("cache_meta_info", null, null, null, null, null, null)) {
            for (int i = 0; i < c.getCount(); i++) {
                c.moveToNext();
                final String nameSpace = c.getString(0);
                if ("tb.my_pages".equals(nameSpace)) {
                    myPagesTable = c.getString(1);
                }
            }
        }
        parseMyPages(myPagesTable);
        mDb.close();
        return this;
    }

    private void parseMyPages(final String tableName) throws JSONException {
        try (final Cursor c = mDb.query(tableName, null, null, null, null, null, null)) {
            c.moveToNext();
            final String mValue = c.getString(4);
            final JSONObject jsonObject = new JSONObject(mValue);
            final JSONArray followList = jsonObject.optJSONArray("follow_list");
            for (int i = 0; i < followList.length(); i++) {
                final JSONObject follow = followList.optJSONObject(i);
                final String name = follow.getString("name_show");
                follows.add(name);
            }
        }
    }
}
