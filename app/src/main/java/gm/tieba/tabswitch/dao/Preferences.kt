package gm.tieba.tabswitch.dao

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

object Preferences {
    private lateinit var sTsPreferences: SharedPreferences
    private lateinit var sTsConfig: SharedPreferences
    @JvmStatic
    fun init(context: Context) {
        sTsPreferences = context.getSharedPreferences("TS_preferences", Context.MODE_PRIVATE)
        sTsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getAll(): Map<String, *> = sTsPreferences.all

    @JvmStatic
    fun remove(key: String) {
        sTsPreferences.edit().remove(key).apply()
    }

    @JvmStatic
    fun putBoolean(key: String, value: Boolean) {
        sTsPreferences.edit().putBoolean(key, value).apply()
    }

    @JvmStatic
    fun getBoolean(key: String): Boolean = sTsPreferences.getBoolean(key, false)

    @JvmStatic
    fun putString(key: String, value: String) {
        sTsPreferences.edit().putString(key, value).apply()
    }

    @JvmStatic
    fun getString(key: String): String? = sTsPreferences.getString(key, null)

    fun putStringSet(key: String, value: String, isContain: Boolean) {
        val set = getStringSet(key).toMutableSet()
        if (isContain) {
            set.add(value)
        } else {
            set.remove(value)
        }
        sTsPreferences.edit().putStringSet(key, set).apply()
    }

    fun getStringSet(key: String): Set<String> = sTsPreferences.getStringSet(key, emptySet()) ?: emptySet()

    // Config
    @JvmStatic
    fun putEULAAccepted() {
        sTsConfig.edit().putBoolean("EULA", true).apply()
    }

    @JvmStatic
    fun getIsEULAAccepted(): Boolean = sTsConfig.getBoolean("EULA", false)

    @JvmStatic
    fun putAutoSignEnabled() {
        sTsConfig.edit().putBoolean("auto_sign", true).apply()
    }

    @JvmStatic
    fun getIsAutoSignEnabled(): Boolean = sTsConfig.getBoolean("auto_sign", false)

    @JvmStatic
    fun getTransitionAnimationEnabled(): Boolean =
        sTsPreferences.getBoolean("transition_animation", false)

    @JvmStatic
    @SuppressLint("ApplySharedPref")
    fun putPurgeEnabled() {
        sTsConfig.edit().putBoolean("ze", true).commit()
    }

    @JvmStatic
    fun getIsPurgeEnabled(): Boolean = sTsConfig.getBoolean("ze", false)

    @JvmStatic
    @SuppressLint("ApplySharedPref")
    fun putSignature(i: Int) {
        sTsConfig.edit().putInt("signature", i).commit()
    }

    @JvmStatic
    fun getSignature(): Int = sTsConfig.getInt("signature", 0)

    @JvmStatic
    fun putLikeForum(follow: Set<String?>?) {
        sTsConfig.edit().putStringSet("like_forum", follow).apply()
    }

    @JvmStatic
    fun getLikeForum(): Set<String>? = sTsConfig.getStringSet("like_forum", null)

    @JvmStatic
    fun putSignDate() {
        sTsConfig.edit().putInt("sign_date", Calendar.getInstance().get(Calendar.DAY_OF_YEAR)).apply()
    }

    @JvmStatic
    fun getIsSigned(): Boolean =
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR) == sTsConfig.getInt("sign_date", 0)

    @JvmStatic
    @SuppressLint("ApplySharedPref")
    fun commit() {
        sTsConfig.edit().commit()
        sTsPreferences.edit().commit()
    }
}
