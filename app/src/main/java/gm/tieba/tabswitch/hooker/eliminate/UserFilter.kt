package gm.tieba.tabswitch.hooker.eliminate

import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker
import java.util.regex.Pattern

class UserFilter : XposedContext(), IHooker, RegexFilter {

    private val mIds: MutableSet<Any> = HashSet()

    override fun key(): String {
        return "user_filter"
    }

    override fun hook() {
        hookBeforeMethod("tbclient.Personalized.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType) { param ->
            val threadList = XposedHelpers.getObjectField(param.thisObject, "thread_list") as? MutableList<*>
            val pattern = getPattern() ?: return@hookBeforeMethod
            threadList?.removeIf { thread ->
                val author = XposedHelpers.getObjectField(thread, "author")
                val authors = arrayOf(
                    XposedHelpers.getObjectField(author, "name") as String,
                    XposedHelpers.getObjectField(author, "name_show") as String
                )
                authors.any { pattern.matcher(it).find() }
            }
        }

        hookBeforeMethod(
            "tbclient.FrsPage.PageData\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            filterPageData(param.thisObject)
        }

        hookBeforeMethod(
            "tbclient.ThreadList.PageData\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            filterPageData(param.thisObject)
        }

        // 楼层
        hookBeforeMethod(
            "tbclient.PbPage.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val postList = XposedHelpers.getObjectField(param.thisObject, "post_list") as? MutableList<*>
            val pattern = getPattern() ?: return@hookBeforeMethod
            initIdList(param.thisObject, pattern)
            postList?.removeIf { post ->
                (XposedHelpers.getObjectField(post, "floor") as Int != 1
                        && mIds.contains(XposedHelpers.getObjectField(post, "author_id")))
            }
        }

        // 楼中楼：[\u202e|\ud83c\udd10-\ud83c\udd89]
        hookBeforeMethod(
            "tbclient.SubPost\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val subPostList = XposedHelpers.getObjectField(param.thisObject, "sub_post_list") as? MutableList<*>
            subPostList?.removeIf { subPost -> mIds.contains(XposedHelpers.getObjectField(subPost, "author_id")) }
        }

        // 楼层回复
        hookBeforeMethod(
            "tbclient.PbFloor.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val subpostList = XposedHelpers.getObjectField(param.thisObject, "subpost_list") as? MutableList<*>
            val pattern = getPattern() ?: return@hookBeforeMethod
            subpostList?.removeIf { subPost ->
                val author = XposedHelpers.getObjectField(subPost, "author")
                val authors = arrayOf(
                    XposedHelpers.getObjectField(author, "name") as String,
                    XposedHelpers.getObjectField(author, "name_show") as String
                )
                authors.any { pattern.matcher(it).find() }
            }
        }
    }

    private fun filterPageData(pageData: Any) {
        val feedList = XposedHelpers.getObjectField(pageData, "feed_list") as? MutableList<*>
        val pattern = getPattern() ?: return

        feedList?.removeIf { feed ->
            val currFeed = XposedHelpers.getObjectField(feed, "feed")

            currFeed?.let {
                val components = XposedHelpers.getObjectField(currFeed, "components") as? List<*>

                components?.firstOrNull { component ->
                    XposedHelpers.getObjectField(component, "component").toString() == "feed_head"
                }?.let { feedHeadComponent ->
                    val feedHead = XposedHelpers.getObjectField(feedHeadComponent, "feed_head")
                    val mainData = XposedHelpers.getObjectField(feedHead, "main_data") as? List<*>

                    mainData?.any { feedHeadSymbol ->
                        val feedHeadText = XposedHelpers.getObjectField(feedHeadSymbol, "text")
                        val username = feedHeadText?.let { XposedHelpers.getObjectField(it, "text") as? String }
                        username?.let { pattern.matcher(it).find() } ?: false
                    } ?: false
                } ?: false
            } ?: false
        }
    }

    private fun initIdList(thisObject: Any, pattern: Pattern) {
        val userList = XposedHelpers.getObjectField(thisObject, "user_list") as? List<*>
        userList?.forEach { user ->
            val authors = arrayOf(
                XposedHelpers.getObjectField(user, "name") as String,
                XposedHelpers.getObjectField(user, "name_show") as String
            )
            if (authors.any { name: String -> pattern.matcher(name).find() }) {
                mIds.add(XposedHelpers.getObjectField(user, "id"))
            }
        }
    }
}
