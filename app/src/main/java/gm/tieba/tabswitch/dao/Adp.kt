package gm.tieba.tabswitch.dao

import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.Preferences.putLikeForum

object Adp : XposedContext() {
    lateinit var BDUSS: String
    lateinit var tbs: String
    lateinit var account: String

    init {
        refreshAccountData()
        refreshCache()
    }

    fun initialize() {
        refreshAccountData()
        refreshCache()
    }

    private fun refreshAccountData() {
        hookAfterMethod("com.baidu.tbadk.core.data.AccountData", "getBDUSS") { param ->
            BDUSS = param.result as String
        }
        hookAfterMethod("com.baidu.tbadk.core.data.AccountData", "getTbs") { param ->
            tbs = param.result as String
        }
        hookAfterMethod("com.baidu.tbadk.core.data.AccountData", "getAccount") { param ->
            account = param.result as String
        }
    }

    private fun refreshCache() {
        hookBeforeMethod("tbclient.ForumRecommend.DataRes\$Builder", "build", Boolean::class.javaPrimitiveType) { param ->
            val forums: MutableSet<String?> = HashSet()
            val list = XposedHelpers.getObjectField(param.thisObject, "like_forum") as List<*>
            list.forEach { forums.add(XposedHelpers.getObjectField(it, "forum_name") as String) }
            putLikeForum(forums)
        }
    }
}
