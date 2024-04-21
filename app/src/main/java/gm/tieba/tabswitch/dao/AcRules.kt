package gm.tieba.tabswitch.dao

import android.content.Context
import androidx.room.Room.databaseBuilder
import gm.tieba.tabswitch.dao.AcRule.Companion.create
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import java.util.Arrays
import java.util.function.Consumer

object AcRules {
    private const val ACRULES_DATABASE_NAME = "Deobfs.db"
    private lateinit var ruleDao: AcRuleDao
    @JvmStatic
    fun init(context: Context) {
        ruleDao = databaseBuilder(
            context.applicationContext, AcRuleDatabase::class.java, ACRULES_DATABASE_NAME
        )
            .allowMainThreadQueries()
            .build()
            .acRuleDao()
    }

    @JvmStatic
    fun dropAllRules() {
        ruleDao.getAll().forEach { rule ->
            ruleDao.delete(rule)
        }
    }

    @JvmStatic
    fun putRule(matcher: String, clazz: String, method: String) {
        ruleDao.insertAll(create(matcher, clazz, method))
    }

    @JvmStatic
    fun findRule(matcher: Matcher, callback: Callback) {
        ruleDao.loadAllMatch(matcher.toString()).forEach { rule ->
            callback.onRuleFound(rule.matcher, rule.clazz, rule.method)
        }
    }

    @JvmStatic
    fun findRule(matchers: List<Matcher>, callback: Callback) {
        val matcherStrings = matchers.map { it.toString() }.toTypedArray()
        ruleDao.loadAllMatch(*matcherStrings).forEach { rule ->
            callback.onRuleFound(rule.matcher, rule.clazz, rule.method)
        }
    }

    @JvmStatic
    fun findRule(str: String, callback: Callback) {
        ruleDao.loadAllMatch(str).forEach { rule ->
            callback.onRuleFound(rule.matcher, rule.clazz, rule.method)
        }
    }

    @JvmStatic
    fun isRuleFound(matcher: String): Boolean {
        return ruleDao.loadAllMatch(matcher).isNotEmpty()
    }

    fun isRuleFound(vararg matchers: String): Boolean {
        return matchers.all { matcher -> isRuleFound(matcher) }
    }

    interface Callback {
        fun onRuleFound(matcher: String, clazz: String, method: String)
    }
}
