package gm.tieba.tabswitch.dao

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

object Preferences {
    private lateinit var sTsPreferences: SharedPreferences
    private lateinit var sTsConfig: SharedPreferences

    fun init(context: Context) {
        sTsPreferences = context.getSharedPreferences("TS_preferences", Context.MODE_PRIVATE)
        sTsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE)
    }

    fun getAll(): Map<String, *> = sTsPreferences.all

    fun remove(key: String) {
        sTsPreferences.edit().remove(key).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        sTsPreferences.edit().putBoolean(key, value).apply()
    }

    @JvmStatic
    fun getBoolean(key: String): Boolean = sTsPreferences.getBoolean(key, false)

    fun putString(key: String, value: String) {
        sTsPreferences.edit().putString(key, value).apply()
    }

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
    fun putEULAAccepted() {
        sTsConfig.edit().putBoolean("EULA", true).apply()
    }

    fun getIsEULAAccepted(): Boolean = sTsConfig.getBoolean("EULA", false)

    fun putAutoSignEnabled() {
        sTsConfig.edit().putBoolean("auto_sign", true).apply()
    }

    fun getIsAutoSignEnabled(): Boolean = sTsConfig.getBoolean("auto_sign", false)

    fun getTransitionAnimationEnabled(): Boolean =
        sTsPreferences.getBoolean("transition_animation", false)

    @SuppressLint("ApplySharedPref")
    fun putPurgeEnabled() {
        sTsConfig.edit().putBoolean("ze", true).commit()
    }

    fun getIsPurgeEnabled(): Boolean = sTsConfig.getBoolean("ze", false)

    @SuppressLint("ApplySharedPref")
    fun putSignature(i: Int) {
        sTsConfig.edit().putInt("signature", i).commit()
    }

    fun getSignature(): Int = sTsConfig.getInt("signature", 0)

    fun putLikeForum(follow: Set<String?>?) {
        sTsConfig.edit().putStringSet("like_forum", follow).apply()
    }

    fun getLikeForum(): Set<String>? = sTsConfig.getStringSet("like_forum", null)

    fun putSignDate() {
        sTsConfig.edit().putInt("sign_date", Calendar.getInstance()[Calendar.DAY_OF_YEAR]).apply()
    }

    fun getIsSigned(): Boolean =
        Calendar.getInstance()[Calendar.DAY_OF_YEAR] == sTsConfig.getInt("sign_date", 0)

    @SuppressLint("ApplySharedPref")
    fun commit() {
        sTsConfig.edit().commit()
        sTsPreferences.edit().commit()
    }
}
