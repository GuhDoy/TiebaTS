package gm.tieba.tabswitch

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XposedBridge
import java.lang.ref.WeakReference

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
        val context: Context
            get() = checkNotNull(sContextRef.get()) { "ApplicationContext is null" }

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
}