package gm.tieba.tabswitch.hookImpl;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDefItem;
import org.jf.util.IndentingWriter2;
import org.jf.util.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gm.tieba.tabswitch.Hook;

public class AntiConfusionHelper extends Hook {
    public static List<String> matcherList = new ArrayList<>();

    static {
        addMatcher(matcherList);
    }

    public static void addMatcher(List<String> list) {
        //Purify
        list.add("\"c/s/splashSchedule\"");//旧启动广告
        list.add("custom_ext_data");//sdk启动广告
        list.add("\"pic_amount\"");//图片广告
        list.add("\"key_frs_dialog_ad_last_show_time\"");//吧推广弹窗
        list.add("Lcom/baidu/tieba/R$id;->frs_ad_banner:I");//吧推广横幅
        list.add("Lcom/baidu/tieba/R$string;->mark_like:I");//关注作者追帖更简单
        //PurifyEnter
        list.add("Lcom/baidu/tieba/R$id;->square_background:I");//吧广场
        list.add("Lcom/baidu/tieba/R$id;->create_bar_container:I");//创建自己的吧
        //PurifyMy
        list.add("Lcom/baidu/tieba/R$drawable;->icon_pure_topbar_store44_svg:I");//商店
        list.add("Lcom/baidu/tieba/R$id;->function_item_bottom_divider:I");//分割线
        list.add("\"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1\"");//我的ArrayList
        //CreateView
        list.add("Lcom/baidu/tieba/R$id;->navigationBarGoSignall:I");//签到按钮
        //ThreadStore
        list.add("\"c/f/post/threadstore\"");
        //StorageRedirect
        list.add("0x4197d783fc000000L");
        //HookDispatcher
        list.add("Lcom/baidu/tieba/R$id;->new_pb_list:I");//调整字号手势
    }

    static void searchAndSave(ClassDefItem cls, int type, SQLiteDatabase db) throws IOException {
        ClassDataItem.EncodedMethod[] methods = null;
        try {
            if (type == 0) methods = cls.getClassData().getDirectMethods();
            else if (type == 1) methods = cls.getClassData().getVirtualMethods();
        } catch (NullPointerException e) {
            return;
        }
        for (ClassDataItem.EncodedMethod method : methods) {
            Parser parser = new Parser(method.codeItem);
            IndentingWriter2 writer = new IndentingWriter2();
            parser.dump(writer);
            for (int i = 0; i < matcherList.size(); i++) {
                if (writer.getString().contains(matcherList.get(i))) {
                    String clazz = cls.getClassType().getTypeDescriptor();
                    clazz = clazz.substring(clazz.indexOf("L") + 1, clazz.indexOf(";")).replace("/", ".");
                    db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{matcherList.get(i), clazz, method.method.methodName.getStringValue()});
                    return;
                }
            }
        }
    }

    public static List<Map<String, String>> convertDbToMapList(SQLiteDatabase db) {
        List<Map<String, String>> dbDataList = new ArrayList<>();
        Cursor c = db.query("rules", null, null, null, null, null, null);
        for (int j = 0; j < c.getCount(); j++) {
            c.moveToNext();
            Map<String, String> map = new HashMap<>();
            map.put("rule", c.getString(1));
            map.put("class", c.getString(2));
            map.put("method", c.getString(3));
            dbDataList.add(map);
        }
        c.close();
        return dbDataList;
    }

    public static boolean isNeedAntiConfusion(Context context) {
        SharedPreferences tsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
        SharedPreferences sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return !tsConfig.getString("anti-confusion_version", "unknown").equals(sharedPreferences.getString("key_rate_version", "unknown"))
                || sharedPreferences.getString("key_rate_version", "unknown").equals("unknown");
    }
}