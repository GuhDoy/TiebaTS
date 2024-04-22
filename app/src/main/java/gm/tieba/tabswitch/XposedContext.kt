package gm.tieba.tabswitch

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.ref.WeakReference
import java.lang.reflect.Method

abstract class XposedContext {
    companion object {
        @JvmField
        var isModuleBetaVersion = false
        @JvmField
        val exceptions: Map<String, Throwable> = HashMap(0)

        @JvmStatic
        lateinit var sClassLoader: ClassLoader
        @JvmStatic
        lateinit var sPath: String
        @JvmStatic
        lateinit var sAssetManager: AssetManager

        private lateinit var sHandler: Handler
        private lateinit var sContextRef: WeakReference<Context>

        @JvmStatic
        fun getContext(): Context = checkNotNull(sContextRef.get()) { "ApplicationContext is null" }

        @JvmStatic
        protected fun attachBaseContext(context: Context) {
            sContextRef = WeakReference(context.applicationContext)
            sHandler = Handler(Looper.getMainLooper())
        }

        @JvmStatic
        protected fun load(filename: String) {
            val soPaths = Build.SUPPORTED_ABIS.map { abi -> "$sPath!/lib/$abi/lib$filename.so" }
            val errors = mutableListOf<Throwable>()

            for (soPath in soPaths) {
                try {
                    System.load(soPath)
                    return
                } catch (e: UnsatisfiedLinkError) {
                    errors.add(e)
                }
            }

            val linkError = UnsatisfiedLinkError("Failed to load native library: $filename")
            errors.forEach { linkError.addSuppressed(it) }
            XposedBridge.log(linkError)
            throw linkError
        }

        @JvmStatic
        protected fun runOnUiThread(r: Runnable) {
            sHandler.post(r)
        }
    }

    inline fun hookBeforeMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Any?,
        crossinline beforeHook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookMethod(
            className, sClassLoader, methodName, *parameterTypes,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    beforeHook(param)
                }
            }
        )
    }

    inline fun hookAfterMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Any?,
        crossinline afterHook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookMethod(
            className, sClassLoader, methodName, *parameterTypes,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    afterHook(param)
                }
            }
        )
    }

    inline fun hookReplaceMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Any?,
        crossinline replaceHook: (XC_MethodHook.MethodHookParam) -> Any?
    ) {
        XposedHelpers.findAndHookMethod(
            className, sClassLoader, methodName, *parameterTypes,
            object : XC_MethodReplacement() {
                @Throws(Throwable::class)
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    return replaceHook(param)
                }
            }
        )
    }

    inline fun hookBeforeMethod(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Any?,
        crossinline beforeHook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookMethod(
            clazz, methodName, *parameterTypes,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    beforeHook(param)
                }
            }
        )
    }

    inline fun hookAfterMethod(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Any?,
        crossinline afterHook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookMethod(
            clazz, methodName, *parameterTypes,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    afterHook(param)
                }
            }
        )
    }

    inline fun hookReplaceMethod(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Any?,
        crossinline replaceHook: (XC_MethodHook.MethodHookParam) -> Any?
    ) {
        XposedHelpers.findAndHookMethod(
            clazz, methodName, *parameterTypes,
            object : XC_MethodReplacement() {
                @Throws(Throwable::class)
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    return replaceHook(param)
                }
            }
        )
    }

    inline fun hookBeforeMethod(
        method: Method,
        crossinline beforeHook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedBridge.hookMethod(
            method,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    beforeHook(param)
                }
            }
        )
    }

    inline fun hookAfterMethod(
        method: Method,
        crossinline afterHook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedBridge.hookMethod(
            method,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    afterHook(param)
                }
            }
        )
    }

    inline fun hookReplaceMethod(
        method: Method,
        crossinline replaceHook: (XC_MethodHook.MethodHookParam) -> Any?
    ) {
        XposedBridge.hookMethod(
            method,
            object : XC_MethodReplacement() {
                @Throws(Throwable::class)
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    return replaceHook(param)
                }
            }
        )
    }

    inline fun hookBeforeConstructor(
        className: String,
        vararg parameterTypes: Any?,
        crossinline beforeHook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookConstructor(
            className, sClassLoader, *parameterTypes,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    beforeHook(param)
                }
            }
        )
    }

    inline fun hookAfterConstructor(
        className: String,
        vararg parameterTypes: Any?,
        crossinline afterHook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookConstructor(
            className, sClassLoader, *parameterTypes,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    afterHook(param)
                }
            }
        )
    }

    inline fun hookBeforeConstructor(
        clazz: Class<*>,
        vararg parameterTypes: Any?,
        crossinline beforeHook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookConstructor(
            clazz, *parameterTypes,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    beforeHook(param)
                }
            }
        )
    }

    inline fun hookAfterConstructor(
        clazz: Class<*>,
        vararg parameterTypes: Any?,
        crossinline afterHook: (XC_MethodHook.MethodHookParam) -> Unit
    ) {
        XposedHelpers.findAndHookConstructor(
            clazz, *parameterTypes,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    afterHook(param)
                }
            }
        )
    }
}
