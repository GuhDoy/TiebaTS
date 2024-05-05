@file:JvmName("DisplayUtils")

package gm.tieba.tabswitch.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.WindowManager
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import kotlin.math.roundToInt
import kotlin.system.exitProcess

fun isLightMode(context: Context): Boolean {
    return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO
}

fun restart(activity: Activity) {
    val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(it)
        exitProcess(0)
    }
}

fun getTbSkin(context: Context): String {
    //Lcom/baidu/tbadk/core/TbadkCoreApplication;->getSkinType()I
    val skinType: Int = try {
        val instance = XposedHelpers.callStaticMethod(
            XposedHelpers.findClass(
                "com.baidu.tbadk.core.TbadkCoreApplication",
                XposedContext.sClassLoader
            ), "getInst"
        )
        XposedHelpers.callMethod(instance, "getSkinType") as Int
    } catch (e: Exception) {
        XposedBridge.log(e)
        val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (settings.getBoolean("key_is_follow_system_mode", false)) {
            return if (isLightMode(context)) "" else "_2"
        } else {
            val commonSettings = context.getSharedPreferences(
                "common_settings", Context.MODE_PRIVATE
            )
            commonSettings.getString("skin_", "0")?.toIntOrNull() ?: 0
        }
    }
    return when (skinType) {
        1, 4 -> "_2"
        else -> ""
    }
}

fun dipToPx(context: Context, dipValue: Float): Int {
    val scale = context.resources.displayMetrics.density
    return (dipValue * scale).roundToInt()
}

fun pxToDip(context: Context, pxValue: Float): Int {
    val scale = context.resources.displayMetrics.density
    return (pxValue / scale).roundToInt()
}

fun getDisplayWidth(context: Context): Int? {
    return context.resources?.displayMetrics?.widthPixels
}

fun fixAlertDialogWidth(alert: AlertDialog) {
    alert.window?.let {
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(it.attributes)
        getDisplayWidth(XposedContext.getContext())?.let { displayWidth ->
            layoutParams.width = displayWidth
        }
        it.attributes = layoutParams
    }
}

fun getDialogTheme(context: Context): Int =
    if (isLightMode(context)) android.R.style.Theme_DeviceDefault_Light_Dialog_Alert else android.R.style.Theme_DeviceDefault_Dialog_Alert

fun getDialogTheme(isLightMode: Boolean): Int =
    if (isLightMode) android.R.style.Theme_DeviceDefault_Light_Dialog_Alert else android.R.style.Theme_DeviceDefault_Dialog_Alert
