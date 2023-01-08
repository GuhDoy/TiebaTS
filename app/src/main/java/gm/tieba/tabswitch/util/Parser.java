package gm.tieba.tabswitch.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
                if (!name.equals("PERSON_TAB_AVAIBLE") &&
                        !name.equals("IS_BACK_CLOSE_ALL_ACTIVITY") &&
                        !name.equals("IS_MAIN_TAB_SPLASH_SHOW")) {
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
}
