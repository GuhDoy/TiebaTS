package gm.tieba.tabswitch.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RulesDbHelper extends SQLiteOpenHelper {
    public RulesDbHelper(Context context) {
        super(context, "Rules.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists rules(id integer primary key autoincrement, rule varchar(255), class varchar(255), method varchar(255))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
    }
}
