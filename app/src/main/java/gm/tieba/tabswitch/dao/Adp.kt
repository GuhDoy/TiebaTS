package gm.tieba.tabswitch.dao

import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.Preferences.putLikeForum

object Adp : XposedContext() {
    var BDUSS: String? = null
    var tbs: String? = null
    var account: String? = null

    fun initializeAdp() {
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
        hookBeforeMethod("tbclient.ForumRecommend.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType) { param ->
            val forums: MutableSet<String?> = HashSet()
            val likeForumList = XposedHelpers.getObjectField(param.thisObject, "like_forum") as? List<*>
            likeForumList?.forEach { forums.add(XposedHelpers.getObjectField(it, "forum_name") as String) }
            putLikeForum(forums)
        }
    }
}
