package gm.tieba.tabswitch.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;

public class ReflectUtils extends BaseHooker {
    private static final Map<String, Field> sFieldCache = new HashMap<>();

    public static int getId(String fieldName) {
        return XposedHelpers.getStaticIntField(XposedHelpers.findClass(
                "com.baidu.tieba.R$id", sClassLoader), fieldName);
    }

    public static int getColor(String fieldName) {
        int colorId = XposedHelpers.getStaticIntField(XposedHelpers.findClass(
                "com.baidu.tieba.R$color", sClassLoader),
                fieldName + DisplayUtils.getTbSkin(getContext()));
        return getContext().getColor(colorId);
    }

    public static float getDimen(String fieldName) {
        int dimenId = XposedHelpers.getStaticIntField(XposedHelpers.findClass(
                "com.baidu.tieba.R$dimen", sClassLoader), fieldName);
        return getContext().getResources().getDimension(dimenId);
    }

    public static float getDimenDip(String fieldName) {
        return DisplayUtils.pxToDip(getContext(), ReflectUtils.getDimen(fieldName));
    }

    public static int getDrawableId(String fieldName) {
        return XposedHelpers.getStaticIntField(XposedHelpers.findClass(
                "com.baidu.tieba.R$drawable", sClassLoader), fieldName);
    }

    public static Field findField(Object instance, String className) {
        String fullFieldName = instance.getClass().getName() + '#' + className;
        try {
            if (sFieldCache.containsKey(fullFieldName)) {
                return sFieldCache.get(fullFieldName);
            } else {
                for (Field field : instance.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    Object objField = field.get(instance);
                    if (objField != null && Objects.equals(objField.getClass().getName(), className)) {
                        sFieldCache.put(fullFieldName, field);
                        return field;
                    }
                }
                throw new NoSuchFieldException();
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new NoSuchFieldError(fullFieldName + " field not found");
        }
    }

    public static Object getObjectField(Object instance, String className) {
        try {
            return findField(instance, className).get(instance);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setObjectField(Object instance, String className, Object value) {
        try {
            findField(instance, className).set(instance, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object callMethod(Method method, Object instance, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            XposedBridge.log(e);
            throw new IllegalArgumentException(e);
        }
    }

    public static Object callStaticMethod(Method method, Object... args) {
        return callMethod(method, null, args);
    }
}
