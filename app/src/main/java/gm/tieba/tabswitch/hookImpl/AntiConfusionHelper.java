package gm.tieba.tabswitch.hookImpl;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDefItem;
import org.jf.util.IndentingWriter2;
import org.jf.util.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.Hook;

public class AntiConfusionHelper extends Hook {
    public static List<Map<String, String>> convertDbToMapList(SQLiteDatabase db) {
        List<Map<String, String>> dbDataList = new ArrayList<>();
        try {
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
        } catch (SQLiteException e) {
            XposedBridge.log(e);
        }
        return dbDataList;
    }

    static boolean searchAndUpdate(ClassDefItem cls, int type) throws IOException {
        ClassDataItem.EncodedMethod[] methods = null;
        try {
            if (type == 0) methods = cls.getClassData().getDirectMethods();
            else if (type == 1) methods = cls.getClassData().getVirtualMethods();
        } catch (NullPointerException e) {
            return false;
        }
        for (ClassDataItem.EncodedMethod method : methods) {
            Parser parser = new Parser(method.codeItem);
            IndentingWriter2 writer = new IndentingWriter2();
            parser.dump(writer);
            for (int i = 0; i < ruleMapList.size(); i++) {
                Map<String, String> map = ruleMapList.get(i);
                if (writer.getString().contains(Objects.requireNonNull(map.get("rule")))) {
                    String clazz = cls.getClassType().getTypeDescriptor();
                    clazz = clazz.substring(clazz.indexOf("L") + 1, clazz.indexOf(";")).replace("/", ".");
                    map.put("class", clazz);
                    map.put("method", method.method.methodName.getStringValue());
                    ruleMapList.set(i, map);
                    return true;
                }
            }
        }
        return false;
    }

    static void putMapListToDb(SQLiteDatabase db) {
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            db.execSQL("update rules set class=? where rule=?", new Object[]{map.get("class"), map.get("rule")});
            db.execSQL("update rules set method=? where rule=?", new Object[]{map.get("method"), map.get("rule")});
        }
    }
}