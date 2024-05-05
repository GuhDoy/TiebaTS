@file:JvmName("ReflectUtils")

package gm.tieba.tabswitch.util

import android.app.Activity
import android.app.Application
import androidx.annotation.ColorInt
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

fun getR(innerClassName: String, fieldName: String): Int {
    return XposedContext.getContext().resources
        .getIdentifier(fieldName, innerClassName, XposedContext.getContext().packageName)
}

fun getId(fieldName: String): Int {
    return getR("id", fieldName)
}

@ColorInt
fun getColor(fieldName: String): Int {
    return XposedContext.getContext().getColor(
        getR("color", fieldName + getTbSkin(XposedContext.getContext()))
    )
}

fun getDimen(fieldName: String): Float {
    when (fieldName) {
        "ds10" -> return dipToPx(XposedContext.getContext(), 5f).toFloat()
        "ds20" -> return dipToPx(XposedContext.getContext(), 10f).toFloat()
        "ds30" -> return dipToPx(XposedContext.getContext(), 15f).toFloat()
        "ds32" -> return dipToPx(XposedContext.getContext(), 16f).toFloat()
        "ds140" -> return dipToPx(XposedContext.getContext(), 70f).toFloat()
    }
    return XposedContext.getContext().resources.getDimension(getR("dimen", fieldName))
}

fun getDimenDip(fieldName: String): Float {
    when (fieldName) {
        "fontsize22" -> return 11f
        "fontsize28" -> return 14f
        "fontsize36" -> return 18f
    }
    return pxToDip(XposedContext.getContext(), getDimen(fieldName)).toFloat()
}

fun getDrawableId(fieldName: String): Int {
    return getR("drawable", fieldName)
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
fun <T> getObjectField(instance: Any?, type: Class<T>): T? {
    return try {
        type.cast(XposedHelpers.findFirstFieldByExactType(instance?.javaClass, type)[instance])
    } catch (e: IllegalAccessException) {
        XposedBridge.log(e)
        throw IllegalAccessError(e.message)
    }
}

fun getObjectField(instance: Any?, className: String): Any? {
    return try {
        XposedHelpers.findFirstFieldByExactType(
            instance?.javaClass,
            XposedHelpers.findClass(className, XposedContext.sClassLoader)
        )[instance]
    } catch (e: IllegalAccessException) {
        XposedBridge.log(e)
        throw IllegalAccessError(e.message)
    }
}

fun setObjectField(instance: Any?, type: Class<*>, value: Any?) {
    try {
        XposedHelpers.findFirstFieldByExactType(instance?.javaClass, type)[instance] = value
    } catch (e: IllegalAccessException) {
        XposedBridge.log(e)
        throw IllegalAccessError(e.message)
    }
}

fun setObjectField(instance: Any?, className: String, value: Any?) {
    try {
        XposedHelpers.findFirstFieldByExactType(
            instance?.javaClass,
            XposedHelpers.findClass(className, XposedContext.sClassLoader)
        )[instance] = value
    } catch (e: IllegalAccessException) {
        XposedBridge.log(e)
        throw IllegalAccessError(e.message)
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
fun getObjectField(instance: Any?, position: Int): Any? {
    return try {
        val field = instance?.javaClass?.declaredFields?.get(position)
        field?.isAccessible = true
        field?.get(instance)
    } catch (e: IllegalAccessException) {
        XposedBridge.log(e)
        throw IllegalAccessError(e.message)
    }
}

fun setObjectField(instance: Any?, position: Int, value: Any?) {
    try {
        val field = instance?.javaClass?.declaredFields?.get(position)
        field?.isAccessible = true
        field?.set(instance, value)
    } catch (e: IllegalAccessException) {
        XposedBridge.log(e)
        throw IllegalAccessError(e.message)
    }
}

fun findFirstMethodByExactType(cls: Class<*>, vararg paramTypes: Class<*>): Method {
    return cls.declaredMethods.firstOrNull { method ->
        method.parameterTypes.contentEquals(paramTypes)
    } ?: throw NoSuchMethodError(paramTypes.contentToString())
}

fun findFirstMethodByExactType(className: String, vararg paramTypes: Class<*>): Method {
    return findFirstMethodByExactType(
        XposedHelpers.findClass(className, XposedContext.sClassLoader),
        *paramTypes
    )
}

fun findFirstMethodByExactReturnType(cls: Class<*>, returnType: Class<*>): Method {
    return cls.declaredMethods.firstOrNull { method ->
        method.returnType == returnType
    } ?: throw NoSuchMethodError(returnType.toString())
}

fun findFirstMethodByExactReturnType(className: String, returnType: Class<*>): Method {
    return findFirstMethodByExactReturnType(
        XposedHelpers.findClass(className, XposedContext.sClassLoader),
        returnType
    )
}

fun callMethod(method: Method, instance: Any?, vararg args: Any?): Any? {
    return try {
        method.isAccessible = true
        method.invoke(instance, *args)
    } catch (e: IllegalAccessException) {
        XposedBridge.log(e)
        throw IllegalArgumentException(e)
    } catch (e: InvocationTargetException) {
        XposedBridge.log(e)
        throw IllegalArgumentException(e)
    }
}

fun callStaticMethod(method: Method, vararg args: Any?): Any? {
    return callMethod(method, null, *args)
}

fun getTbadkCoreApplicationInst(): Application = XposedHelpers.callStaticMethod(
    XposedHelpers.findClass(
        "com.baidu.tbadk.core.TbadkCoreApplication",
        XposedContext.sClassLoader
    ),
    "getInst"
) as Application

fun getCurrentActivity(): Activity = XposedHelpers.callMethod(
    getTbadkCoreApplicationInst(),
    "getCurrentActivity"
) as Activity
