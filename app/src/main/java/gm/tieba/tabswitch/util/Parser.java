package gm.tieba.tabswitch.util;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;

public class Parser extends XposedContext {
    public static List<String> parseMainTabActivityConfig() {
        List<String> mainTabActivityConfig = new ArrayList<>();
        try {
            for (Field field : XposedHelpers.findClass("com.baidu.tbadk.core.atomData.MainTabActivityConfig",
                    sClassLoader).getDeclaredFields()) {
                if (!field.getType().equals(boolean.class)) continue;
                String name = field.getName();
                if (!name.equals("PERSON_TAB_AVAIBLE") && !name.equals("IS_BACK_CLOSE_ALL_ACTIVITY")) {
                    mainTabActivityConfig.add(field.getName());
                }
            }
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
        return mainTabActivityConfig;
    }

    public static String parsePbContent(Object instance, String fieldName) {
        List<?> contents = (List<?>) XposedHelpers.getObjectField(instance, fieldName);
        StringBuilder pbContent = new StringBuilder();
        for (int i = 0; i < contents.size(); i++) {
            pbContent.append(XposedHelpers.getObjectField(contents.get(i), "text"));
        }
        return pbContent.toString();
    }

    public static Map<Integer, String> resolveIdentifier(List<String> source) {
        var result = new HashMap<Integer, String>(source.size());
        var resources = getContext().getResources();
        var defPackage = getContext().getPackageName();
        source.forEach(it -> {
            var defType = StringsKt.substringBetween(it, "R$", ";->", "");
            if (!TextUtils.isEmpty(defType)) {
                var name = StringsKt.substringBetween(it, ";->", ":I", "");
                var identifier = resources.getIdentifier(name, defType, defPackage);
                if (identifier != 0) {
                    result.put(identifier, it);
                } else {
                    // TODO
                }
            }
        });
        return result;
    }
}
