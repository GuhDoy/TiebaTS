package gm.tieba.tabswitch.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XposedHelpers;

public class Reflect {
    public static Object getObjectField(Object instance, String className) throws Throwable {
        for (Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.get(instance) != null && Objects.equals(field.get(instance).getClass().getName(), className)) {
                return field.get(instance);
            }
        }
        throw new NoSuchFieldException(className + " field not found");
    }

    public static void setObjectField(Object instance, String className, Object value) throws Throwable {
        for (Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.get(instance) != null && Objects.equals(field.get(instance).getClass().getName(), className)) {
                field.set(instance, value);
            }
        }
        throw new NoSuchFieldException(className + " field not found");
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