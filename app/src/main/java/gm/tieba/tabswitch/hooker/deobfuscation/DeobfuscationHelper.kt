package gm.tieba.tabswitch.hooker.deobfuscation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import de.robv.android.xposed.XposedBridge
import gm.tieba.tabswitch.XposedContext.Companion.hookBeforeMethod
import gm.tieba.tabswitch.dao.Preferences.getSignature
import gm.tieba.tabswitch.util.restart
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.ZipFile
import kotlin.math.max

object DeobfuscationHelper {
    private const val SIGNATURE_DATA_START_OFFSET = 32
    private const val SIGNATURE_SIZE = 20
    lateinit var sCurrentTbVersion: String

    fun calcSignature(dataStoreInput: InputStream): ByteArray {
        val md: MessageDigest = try {
            MessageDigest.getInstance("SHA-1")
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException(ex)
        }

        dataStoreInput.skip(SIGNATURE_DATA_START_OFFSET.toLong())
        val buffer = ByteArray(4 * 1024)

        dataStoreInput.use { input ->
            generateSequence { input.read(buffer).takeIf { it >= 0 } }
                .forEach { bytesRead -> md.update(buffer, 0, bytesRead) }
        }

        return md.digest().also { signature ->
            check(signature.size == SIGNATURE_SIZE) { "unexpected digest write: ${signature.size} bytes" }
        }
    }

    fun isVersionChanged(context: Context): Boolean {
        val tsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE)
        return tsConfig.getString("deobfs_version", "unknown") != getTbVersion(context)
    }

    fun isDexChanged(context: Context): Boolean {
        return try {
            ZipFile(File(context.packageResourcePath)).use { zipFile ->
                zipFile.getEntry("classes.dex")?.let { entry ->
                    zipFile.getInputStream(entry).use { inputStream ->
                        calcSignature(inputStream).contentHashCode() != getSignature()
                    }
                } ?: false
            }
        } catch (e: IOException) {
            XposedBridge.log(e)
            false
        }
    }

    @Suppress("DEPRECATION")
    fun getTbVersion(context: Context): String {
        val pm = context.packageManager
        try {
            val applicationInfo = pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            return when (applicationInfo.metaData["versionType"] as Int?) {
                3 -> pm.getPackageInfo(context.packageName, 0).versionName
                2 -> applicationInfo.metaData["grayVersion"].toString()
                1 -> applicationInfo.metaData["subVersion"].toString()
                else -> throw PackageManager.NameNotFoundException("unknown tb version")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            XposedBridge.log(e)
            return "unknown"
        }
    }

    @SuppressLint("ApplySharedPref")
    fun saveAndRestart(activity: Activity, version: String, trampoline: Class<*>?) {
        activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE)
            .edit()
            .putString("deobfs_version", version)
            .commit()

        trampoline?.let {
            hookBeforeMethod(it, "onCreate", Bundle::class.java) { param ->
                restart(param.thisObject as Activity)
            }
            activity.startActivity(
                Intent(activity, it).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
        } ?: restart(activity)
    }

    // Adapted from https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
    fun isTbSatisfyVersionRequirement(requiredVersion: String): Boolean {
        val currParts = sCurrentTbVersion.split(".")
        val reqParts = requiredVersion.split(".")

        val length = max(currParts.size, reqParts.size)
        for (i in 0 until length) {
            try {
                val currPart = (currParts.getOrNull(i))?.toInt() ?: 0
                val reqPart = (reqParts.getOrNull(i))?.toInt() ?: 0
                if (currPart != reqPart) {
                    return currPart > reqPart
                }
            } catch (e: NumberFormatException) {
                return false
            }
        }
        return true
    }

    // Inclusive of both ends
    fun isTbBetweenVersionRequirement(lower: String, upper: String): Boolean {
        return (isTbSatisfyVersionRequirement(lower)
                && (!isTbSatisfyVersionRequirement(upper) || sCurrentTbVersion == upper))
    }
}
