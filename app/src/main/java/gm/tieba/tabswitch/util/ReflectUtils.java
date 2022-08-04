package gm.tieba.tabswitch.util;

import androidx.annotation.ColorInt;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;

public class ReflectUtils extends XposedContext {
    public static int getR(String innerClassName, String fieldName) {
        return getContext().getResources()
                .getIdentifier(fieldName, innerClassName, getContext().getPackageName());
    }

    public static int getId(String fieldName) {
        return getR("id", fieldName);
    }

    @ColorInt
    public static int getColor(String fieldName) {
        return getContext().getColor(
                getR("color", fieldName + DisplayUtils.getTbSkin(getContext())));
    }

    public static float getDimen(String fieldName) {
        switch (fieldName) {
            case "ds10":
                return DisplayUtils.dipToPx(getContext(), 5F);
            case "ds30":
                return DisplayUtils.dipToPx(getContext(), 15F);
            case "ds32":
                return DisplayUtils.dipToPx(getContext(), 16F);
            case "ds140":
                return DisplayUtils.dipToPx(getContext(), 70F);
        }
        return getContext().getResources().getDimension(getR("dimen", fieldName));
    }

    public static float getDimenDip(String fieldName) {
        switch (fieldName) {
            case "fontsize28":
                return 14F;
            case "fontsize36":
                return 18F;
        }
        return DisplayUtils.pxToDip(getContext(), getDimen(fieldName));
    }

    public static int getDrawableId(String fieldName) {
        return getR("drawable", fieldName);
    }

    /**
     * Returns the first field of the given type in a class.
     * Might be useful for Proguard'ed classes to identify fields with unique types.
     *
     * @param clazz The class which either declares or inherits the field.
     * @param type  The type of the field.
     * @return A reference to the first field of the given type.
     * @throws NoSuchFieldError In case no matching field was not found.
     */
    public static <T> T getObjectField(Object instance, Class<T> type) {
        try {
            return type.cast(XposedHelpers.findFirstFieldByExactType(instance.getClass(), type)
                    .get(instance));
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object getObjectField(Object instance, String className) {
        try {
            return XposedHelpers.findFirstFieldByExactType(instance.getClass(), XposedHelpers.findClass(className, sClassLoader))
                    .get(instance);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setObjectField(Object instance, Class<?> type, Object value) {
        try {
            XposedHelpers.findFirstFieldByExactType(instance.getClass(), type)
                    .set(instance, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setObjectField(Object instance, String className, Object value) {
        try {
            XposedHelpers.findFirstFieldByExactType(instance.getClass(), XposedHelpers.findClass(className, sClassLoader))
                    .set(instance, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    /**
     * Returns the field at the given position in a class.
     * Might be useful for Proguard'ed classes to identify fields with fixed position.
     *
     * @param clazz    The class which either declares or inherits the field.
     * @param position The position of the field.
     * @return A reference to the first field of the given type.
     * @throws NoSuchFieldError In case no matching field was not found.
     */
    public static Object getObjectField(Object instance, int position) {
        try {
            var field = instance.getClass().getDeclaredFields()[position];
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setObjectField(Object instance, int position, Object value) {
        try {
            var field = instance.getClass().getDeclaredFields()[position];
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public interface Callback {
        /**
         * @param objField the value of the represented field in object
         * @return True if no further handling is desired
         */
        boolean onFieldFound(Object objField);
    }

    public static void walkField(Object instance, Class<?> cls, Callback handle) {
        try {
            Field[] declaredFields = instance.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                Object objField = field.get(instance);
                if (objField != null && cls.equals(objField.getClass())
                        && handle.onFieldFound(objField)) {
                    return;
                }
            }
            for (Field field : declaredFields) {
                Object objField = field.get(instance);
                if (objField != null && cls.isAssignableFrom(objField.getClass())
                        && handle.onFieldFound(objField)) {
                    return;
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void walkObjectFields(Object instance, Class<?> cls, Callback handle) {
        walkField(instance, cls, handle);
    }

    public static void walkObjectFields(Object instance, String className, Callback handle) {
        walkField(instance, XposedHelpers.findClass(className, sClassLoader), handle);
    }

    public static Method findFirstMethodByExactType(Class<?> cls, Class<?>... paramTypes) {
        for (var method : cls.getDeclaredMethods()) {
            if (Arrays.equals(method.getParameterTypes(), paramTypes)) {
                return method;
            }
        }
        throw new NoSuchMethodError(Arrays.toString(paramTypes));
    }

    public static Method findFirstMethodByExactType(String className, Class<?>... paramTypes) {
        return findFirstMethodByExactType(XposedHelpers.findClass(className, sClassLoader), paramTypes);
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
