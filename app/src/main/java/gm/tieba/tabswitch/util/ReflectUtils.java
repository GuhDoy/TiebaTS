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
    public static int getR(final String innerClassName, final String fieldName) {
        return getContext().getResources()
                .getIdentifier(fieldName, innerClassName, getContext().getPackageName());
    }

    public static int getId(final String fieldName) {
        return getR("id", fieldName);
    }

    @ColorInt
    public static int getColor(final String fieldName) {
        return getContext().getColor(
                getR("color", fieldName + DisplayUtils.getTbSkin(getContext())));
    }

    public static float getDimen(final String fieldName) {
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

    public static float getDimenDip(final String fieldName) {
        switch (fieldName) {
            case "fontsize28":
                return 14F;
            case "fontsize36":
                return 18F;
        }
        return DisplayUtils.pxToDip(getContext(), getDimen(fieldName));
    }

    public static int getDrawableId(final String fieldName) {
        return getR("drawable", fieldName);
    }

    /**
     * Returns the first field of the given type in a class.
     * Might be useful for Proguard'ed classes to identify fields with unique types.
     *
     * @param instance The class which either declares or inherits the field.
     * @param type     The type of the field.
     * @return A reference to the first field of the given type.
     * @throws NoSuchFieldError In case no matching field was not found.
     */
    public static <T> T getObjectField(final Object instance, final Class<T> type) {
        try {
            return type.cast(XposedHelpers.findFirstFieldByExactType(instance.getClass(), type)
                    .get(instance));
        } catch (final IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object getObjectField(final Object instance, final String className) {
        try {
            return XposedHelpers.findFirstFieldByExactType(instance.getClass(), XposedHelpers.findClass(className, sClassLoader))
                    .get(instance);
        } catch (final IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setObjectField(final Object instance, final Class<?> type, final Object value) {
        try {
            XposedHelpers.findFirstFieldByExactType(instance.getClass(), type)
                    .set(instance, value);
        } catch (final IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setObjectField(final Object instance, final String className, final Object value) {
        try {
            XposedHelpers.findFirstFieldByExactType(instance.getClass(), XposedHelpers.findClass(className, sClassLoader))
                    .set(instance, value);
        } catch (final IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    /**
     * Returns the field at the given position in a class.
     * Might be useful for Proguard'ed classes to identify fields with fixed position.
     *
     * @param instance The class which either declares or inherits the field.
     * @param position The position of the field.
     * @return A reference to the first field of the given type.
     * @throws NoSuchFieldError In case no matching field was not found.
     */
    public static Object getObjectField(final Object instance, final int position) {
        try {
            final var field = instance.getClass().getDeclaredFields()[position];
            field.setAccessible(true);
            return field.get(instance);
        } catch (final IllegalAccessException e) {
            XposedBridge.log(e);
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setObjectField(final Object instance, final int position, final Object value) {
        try {
            final var field = instance.getClass().getDeclaredFields()[position];
            field.setAccessible(true);
            field.set(instance, value);
        } catch (final IllegalAccessException e) {
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

    public static void walkField(final Object instance, final Class<?> cls, final Callback handle) {
        try {
            final Field[] declaredFields = instance.getClass().getDeclaredFields();
            for (final Field field : declaredFields) {
                field.setAccessible(true);
                final Object objField = field.get(instance);
                if (objField != null && cls.equals(objField.getClass())
                        && handle.onFieldFound(objField)) {
                    return;
                }
            }
            for (final Field field : declaredFields) {
                final Object objField = field.get(instance);
                if (objField != null && cls.isAssignableFrom(objField.getClass())
                        && handle.onFieldFound(objField)) {
                    return;
                }
            }
        } catch (final IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void walkObjectFields(final Object instance, final Class<?> cls, final Callback handle) {
        walkField(instance, cls, handle);
    }

    public static void walkObjectFields(final Object instance, final String className, final Callback handle) {
        walkField(instance, XposedHelpers.findClass(className, sClassLoader), handle);
    }

    public static Method findFirstMethodByExactType(final Class<?> cls, final Class<?>... paramTypes) {
        for (final var method : cls.getDeclaredMethods()) {
            if (Arrays.equals(method.getParameterTypes(), paramTypes)) {
                return method;
            }
        }
        throw new NoSuchMethodError(Arrays.toString(paramTypes));
    }

    public static Method findFirstMethodByExactType(final String className, final Class<?>... paramTypes) {
        return findFirstMethodByExactType(XposedHelpers.findClass(className, sClassLoader), paramTypes);
    }

    public static Object callMethod(final Method method, final Object instance, final Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(instance, args);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            XposedBridge.log(e);
            throw new IllegalArgumentException(e);
        }
    }

    public static Object callStaticMethod(final Method method, final Object... args) {
        return callMethod(method, null, args);
    }
}
