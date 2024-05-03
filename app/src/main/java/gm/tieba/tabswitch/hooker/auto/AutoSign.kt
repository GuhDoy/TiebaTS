package gm.tieba.tabswitch.hooker.auto

import android.os.Bundle
import de.robv.android.xposed.XposedBridge
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.Adp
import gm.tieba.tabswitch.dao.Preferences.getIsSigned
import gm.tieba.tabswitch.dao.Preferences.putLikeForum
import gm.tieba.tabswitch.dao.Preferences.putSignDate
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.widget.TbToast
import gm.tieba.tabswitch.widget.TbToast.Companion.showTbToast
import java.net.URLEncoder
import kotlin.concurrent.thread

class AutoSign : XposedContext(), IHooker {
    companion object {
        //获取用户所有关注贴吧
        private const val LIKE_URL = "https://tieba.baidu.com/mo/q/newmoindex"

        //获取用户的tbs
        private const val TBS_URL = "http://tieba.baidu.com/dc/common/tbs"

        //贴吧签到接口
        private const val SIGN_URL = "http://c.tieba.baidu.com/c/c/forum/sign"
    }

    private val mFollow = mutableListOf<String>()
    private val mSuccess = mutableListOf<String>()
    private var mTbs: String? = null
    private var mFollowNum = 201

    override fun key(): String {
        return "auto_sign"
    }

    override fun hook() {
        hookAfterMethod(
            "com.baidu.tieba.tblauncher.MainTabActivity",
            "onCreate", Bundle::class.java
        ) { _ ->
            if (!getIsSigned()) {
                thread {
                    val result = main(Adp.BDUSS)
                    if (result.endsWith("全部签到成功")) {
                        putSignDate()
                        putLikeForum(HashSet(mSuccess))
                    }
                    runOnUiThread { showTbToast(result, TbToast.LENGTH_SHORT) }
                }
            }
        }
    }

    private fun main(BDUSS: String?): String {
        if (BDUSS == null) return "暂未获取到 BDUSS"

        AutoSignHelper.setCookie(BDUSS)
        getTbs()
        getFollow()
        runSign()

        val failNum = mFollowNum - mSuccess.size
        val result = "共${mFollowNum}个吧 - 成功：${mSuccess.size} - 失败：$failNum"
        XposedBridge.log(result)
        return if (failNum == 0) "共${mFollowNum}个吧 - 全部签到成功" else result
    }

    private fun getTbs() {
        mTbs = Adp.tbs
        if (mTbs != null) return
        try {
            val jsonObject = AutoSignHelper.get(TBS_URL)
            if ("1" == jsonObject.getString("is_login")) {
                XposedBridge.log("获取tbs成功")
                mTbs = jsonObject.getString("tbs")
            } else XposedBridge.log("获取tbs失败 -- $jsonObject")
        } catch (e: Exception) {
            XposedBridge.log("获取tbs部分出现错误 -- $e")
        }
    }

    private fun getFollow() {
        try {
            val jsonObject = AutoSignHelper.get(LIKE_URL)
            XposedBridge.log("获取贴吧列表成功")

            val jsonArray = jsonObject.getJSONObject("data").getJSONArray("like_forum")
            mFollowNum = jsonArray.length()

            // 获取用户所有关注的贴吧
            for (i in 0 until jsonArray.length()) {
                val forumObject = jsonArray.optJSONObject(i)
                val forumName = forumObject?.getString("forum_name") ?: continue

                when (forumObject.getString("is_sign")) {
                    "0" -> mFollow.add(forumName)
                    else -> mSuccess.add(forumName)
                }
            }
        } catch (e: Exception) {
            XposedBridge.log("获取贴吧列表部分出现错误 -- $e")
        }
    }

    private fun runSign() {
        // 当执行 3 轮所有贴吧还未签到成功就结束操作
        var flag = 3

        try {
            while (mSuccess.size < mFollowNum && flag > 0) {

                mFollow.removeAll { s ->
                    val encodedS = URLEncoder.encode(s, "UTF-8")
                    val body = "kw=$encodedS&tbs=$mTbs&sign=${AutoSignHelper.enCodeMd5("kw=${s}tbs=${mTbs}tiebaclient!!!")}"

                    val post = AutoSignHelper.post(SIGN_URL, body)
                    when (post.getString("error_code")) {
                        "0" -> {
                            mSuccess.add(s)
                            XposedBridge.log("$s: 签到成功")
                            true
                        }
                        else -> {
                            XposedBridge.log("$s: 签到失败")
                            false
                        }
                    }
                }

                if (mSuccess.size != mFollowNum) {
                    Thread.sleep(2500)
                    getTbs()
                }
                flag--
            }
        } catch (e: Exception) {
            XposedBridge.log("签到部分出现错误 -- $e")
        }
    }
}
