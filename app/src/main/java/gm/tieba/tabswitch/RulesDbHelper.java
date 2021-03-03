package gm.tieba.tabswitch;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RulesDbHelper extends SQLiteOpenHelper {
    public RulesDbHelper(Context context) {
        super(context, "Rules.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table rules(id integer primary key autoincrement, rule varchar(255), class varchar(255), method varchar(255))");
        //启动广告
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"\"custom_ext_data\"", "", ""});
        //图片广告
        //必须："recom_ala_info", "app", 可选："goods_info"
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"\"pic_amount\"", "", ""});
        //吧推广弹窗
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"\"key_frs_dialog_ad_last_show_time\"", "", ""});
        //吧推广横幅
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"Lcom/baidu/tieba/R$id;->frs_ad_banner:I", "", ""});
        //吧广场
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"Lcom/baidu/tieba/R$id;->square_background:I", "", ""});
        //创建自己的吧
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"Lcom/baidu/tieba/R$id;->create_bar_container:I", "", ""});
        //商店
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"Lcom/baidu/tieba/R$drawable;->icon_pure_topbar_store44_svg:I", "", ""});
        //分割线
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"Lcom/baidu/tieba/R$id;->function_item_bottom_divider:I", "", ""});
        //我的ArrayList
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"\"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1\"", "", ""});
        //签到按钮
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"Lcom/baidu/tieba/R$id;->navigationBarGoSignall:I", "", ""});
        //存储重定向
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"0x4197d783fc000000L", "", ""});
        //调整字号手势
        db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{"Lcom/baidu/tieba/R$id;->new_pb_list:I", "", ""});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
    }
}