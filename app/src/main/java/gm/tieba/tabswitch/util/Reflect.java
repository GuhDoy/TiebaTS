package gm.tieba.tabswitch.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;

public class Reflect extends BaseHooker {
    public static int getId(String fieldName) throws Throwable {
        return sClassLoader.loadClass("com.baidu.tieba.R$id").getField(fieldName).getInt(null);
    }

    public static int getColor(String fieldName) throws Throwable {
        return getContext().getColor(sClassLoader.loadClass("com.baidu.tieba.R$color")
                .getField(fieldName + DisplayHelper.getTbSkin(getContext())).getInt(null));
    }

    public static float getDimen(String fieldName) throws Throwable {
        return getContext().getResources().getDimension(sClassLoader.loadClass(
                "com.baidu.tieba.R$dimen").getField(fieldName).getInt(null));
    }

    public static float getDimenDip(String fieldName) throws Throwable {
        return DisplayHelper.pxToDip(getContext(), Reflect.getDimen(fieldName));
    }

    public static int getDrawable(String fieldName) throws Throwable {
        return sClassLoader.loadClass("com.baidu.tieba.R$drawable").getField(fieldName)
                .getInt(null);
    }

    public static Object getObjectField(Object instance, String className) throws Throwable {
        for (Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object objField = field.get(instance);
            if (objField != null && Objects.equals(objField.getClass().getName(), className)) {
                return objField;
            }
        }
        throw new NoSuchFieldException(className + " field not found");
    }

    public static void setObjectField(Object instance, String className, Object value)
            throws Throwable {
        for (Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object objField = field.get(instance);
            if (objField != null && Objects.equals(objField.getClass().getName(), className)) {
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
