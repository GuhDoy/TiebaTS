package gm.tieba.tabswitch.hookImpl;

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
    private static final List<String> matcherList = new ArrayList<>();

    static {
        //启动广告
        matcherList.add("\"custom_ext_data\"");
        //图片广告
        //必须："recom_ala_info", "app", 可选："goods_info"
        matcherList.add("\"pic_amount\"");
        //吧推广弹窗
        matcherList.add("\"key_frs_dialog_ad_last_show_time\"");
        //吧推广横幅
        matcherList.add("Lcom/baidu/tieba/R$id;->frs_ad_banner:I");
        //吧广场
        matcherList.add("Lcom/baidu/tieba/R$id;->square_background:I");
        //创建自己的吧
        matcherList.add("Lcom/baidu/tieba/R$id;->create_bar_container:I");
        //商店
        matcherList.add("Lcom/baidu/tieba/R$drawable;->icon_pure_topbar_store44_svg:I");
        //分割线
        matcherList.add("Lcom/baidu/tieba/R$id;->function_item_bottom_divider:I");
        //我的ArrayList
        matcherList.add("\"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1\"");
        //签到按钮
        matcherList.add("Lcom/baidu/tieba/R$id;->navigationBarGoSignall:I");
        //存储重定向
        matcherList.add("0x4197d783fc000000L");
        //调整字号手势
        matcherList.add("Lcom/baidu/tieba/R$id;->new_pb_list:I");
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

    public static List<String> getMatcherList() {
        return matcherList;
    }
}