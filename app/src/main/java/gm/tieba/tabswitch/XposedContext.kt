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
import de.robv.android.xposed.callbacks.XCallback
import java.lang.ref.WeakReference
import java.lang.reflect.Method

abstract class XposedContext {

    companion object {
        val isModuleBetaVersion = BuildConfig.VERSION_NAME.contains("alpha") || BuildConfig.VERSION_NAME.contains("beta")
        val exceptions: MutableMap<String, Throwable> = HashMap(0)

        lateinit var sClassLoader: ClassLoader
        lateinit var sPath: String
        lateinit var sAssetManager: AssetManager

        private lateinit var sHandler: Handler
        private lateinit var sContextRef: WeakReference<Context>

        @JvmStatic
        fun getContext(): Context = checkNotNull(sContextRef.get()) { "ApplicationContext is null" }

        fun attachBaseContext(context: Context) {
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

        fun runOnUiThread(r: Runnable) {
            sHandler.post(r)
        }

        fun findClass(className: String): Class<*> = XposedHelpers.findClass(className, sClassLoader)

        inline fun hookBeforeMethod(
            className: String,
            methodName: String,
            vararg parameterTypes: Any?,
            crossinline beforeHook: (XC_MethodHook.MethodHookParam) -> Unit
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookMethod(
                className, sClassLoader, methodName, *parameterTypes,
                object : XC_MethodHook() {
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
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookMethod(
                className, sClassLoader, methodName, *parameterTypes,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        afterHook(param)
                    }
                }
            )
        }

        inline fun hookAfterMethodPriority(
            className: String,
            methodName: String,
            vararg parameterTypes: Any?,
            crossinline afterHook: (XC_MethodHook.MethodHookParam) -> Unit
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookMethod(
                className, sClassLoader, methodName, *parameterTypes,
                object : XC_MethodHook(XCallback.PRIORITY_LOWEST) {
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
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookMethod(
                className, sClassLoader, methodName, *parameterTypes,
                object : XC_MethodReplacement() {
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
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookMethod(
                clazz, methodName, *parameterTypes,
                object : XC_MethodHook() {
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
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookMethod(
                clazz, methodName, *parameterTypes,
                object : XC_MethodHook() {
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
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookMethod(
                clazz, methodName, *parameterTypes,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        return replaceHook(param)
                    }
                }
            )
        }

        inline fun hookBeforeMethod(
            method: Method,
            crossinline beforeHook: (XC_MethodHook.MethodHookParam) -> Unit
        ): XC_MethodHook.Unhook {
            return XposedBridge.hookMethod(
                method,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        beforeHook(param)
                    }
                }
            )
        }

        inline fun hookAfterMethod(
            method: Method,
            crossinline afterHook: (XC_MethodHook.MethodHookParam) -> Unit
        ): XC_MethodHook.Unhook {
            return XposedBridge.hookMethod(
                method,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        afterHook(param)
                    }
                }
            )
        }

        inline fun hookReplaceMethod(
            method: Method,
            crossinline replaceHook: (XC_MethodHook.MethodHookParam) -> Any?
        ): XC_MethodHook.Unhook {
            return XposedBridge.hookMethod(
                method,
                object : XC_MethodReplacement() {
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
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookConstructor(
                className, sClassLoader, *parameterTypes,
                object : XC_MethodHook() {
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
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookConstructor(
                className, sClassLoader, *parameterTypes,
                object : XC_MethodHook() {
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
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookConstructor(
                clazz, *parameterTypes,
                object : XC_MethodHook() {
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
        ): XC_MethodHook.Unhook {
            return XposedHelpers.findAndHookConstructor(
                clazz, *parameterTypes,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        afterHook(param)
                    }
                }
            )
        }
    }
}
