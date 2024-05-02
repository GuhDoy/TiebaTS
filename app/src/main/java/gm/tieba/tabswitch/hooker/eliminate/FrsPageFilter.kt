package gm.tieba.tabswitch.hooker.eliminate

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker

class FrsPageFilter : XposedContext(), IHooker, RegexFilter {
    override fun key(): String {
        return "frs_page_filter"
    }

    @Throws(Throwable::class)
    override fun hook() {
        hookBeforeMethod(
            "tbclient.FrsPage.PageData\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param-> filterPageData(param.thisObject) }
        hookBeforeMethod(
            "tbclient.ThreadList.PageData\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param-> filterPageData(param.thisObject) }
    }

    private fun filterPageData(pageData: Any) {
        val feedList = XposedHelpers.getObjectField(pageData, "feed_list") as? MutableList<*>
        val pattern = getPattern() ?: return

        feedList?.removeIf { feedItem ->
            val currFeed = XposedHelpers.getObjectField(feedItem, "feed")

            currFeed?.let { feed ->
                val businessInfo = XposedHelpers.getObjectField(feed, "business_info") as? List<*>

                businessInfo?.any { feedKV ->
                    val currKey = XposedHelpers.getObjectField(feedKV, "key").toString()
                    if (currKey == "title" || currKey == "abstract") {
                        val str = XposedHelpers.getObjectField(feedKV, "value").toString()
                        pattern.matcher(str).find()
                    } else {
                        false
                    }
                } ?: false
            } ?: false
        }
    }
}
