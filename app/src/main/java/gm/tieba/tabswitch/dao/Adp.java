package gm.tieba.tabswitch.dao;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;

public class Adp extends BaseHooker {
    private static Adp adp;
    public String BDUSS;
    public String tbs;
    public String account;
    public Set<String> follows = new HashSet<>();
    private SQLiteDatabase mDb;

    public Adp(ClassLoader classLoader, Context context, Resources res) {
        super(classLoader, context, res);
        adp = this;
        getAccountData();
        refreshCache();
    }

    public static Adp getInstance() {
        return adp;
    }

    private void getAccountData() {
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.data.AccountData", sClassLoader,
                "getBDUSS", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        BDUSS = (String) param.getResult();
                    }
                });
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.data.AccountData", sClassLoader,
                "getTbs", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        tbs = (String) param.getResult();
                    }
                });
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.data.AccountData", sClassLoader,
                "getAccount", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        account = (String) param.getResult();
                    }
                });
    }

    private void refreshCache() {
        XposedHelpers.findAndHookMethod("tbclient.ForumRecommend.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Set<String> forums = new HashSet<>();
                List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "like_forum");
                if (list == null) return;
                for (int i = 0; i < list.size(); i++) {
                    forums.add((String) XposedHelpers.getObjectField(list.get(i), "forum_name"));
                }
                Preferences.putLikeForum(forums);
            }
        });
    }

    public synchronized Adp parseDatabase() {
        String myPagesTable = null;
        mDb = getContext().openOrCreateDatabase("baidu_adp.db", Context.MODE_PRIVATE,
                null);
        Cursor cacheMetaInfo = mDb.query("cache_meta_info", null, null, null, null, null, null);
        for (int i = 0; i < cacheMetaInfo.getCount(); i++) {
            cacheMetaInfo.moveToNext();
            String nameSpace = cacheMetaInfo.getString(0);
            if ("tb.my_pages".equals(nameSpace)) {
                myPagesTable = cacheMetaInfo.getString(1);
            }
        }
        cacheMetaInfo.close();
        parseMyPages(myPagesTable);
        mDb.close();
        return this;
    }

    private void parseMyPages(String tableName) {
        Cursor myPages = mDb.query(tableName, null, null, null, null, null, null);
        myPages.moveToNext();
        String mValue = myPages.getString(4);
        try {
            JSONObject jsonObject = new JSONObject(mValue);
            JSONArray followList = jsonObject.optJSONArray("follow_list");
            for (int i = 0; i < followList.length(); i++) {
                JSONObject follow = followList.optJSONObject(i);
                String name = follow.getString("name_show");
                follows.add(name);
            }
        } catch (JSONException e) {
            XposedBridge.log(e);
        }
        myPages.close();
    }
}
