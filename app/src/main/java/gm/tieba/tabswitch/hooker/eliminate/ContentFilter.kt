package gm.tieba.tabswitch.hooker.eliminate

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.util.parsePbContent

class ContentFilter : XposedContext(), IHooker, RegexFilter {
    override fun key(): String {
        return "content_filter"
    }

    @Throws(Throwable::class)
    override fun hook() {
        // 楼层
        hookBeforeMethod(
            "tbclient.PbPage.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val postList = XposedHelpers.getObjectField(param.thisObject, "post_list") as? MutableList<*>
            val pattern = getPattern() ?: return@hookBeforeMethod
            postList?.removeIf { o: Any? ->
                (XposedHelpers.getObjectField(o, "floor") as Int != 1
                        && pattern.matcher(parsePbContent(o, "content")).find())
            }
        }

        // 楼中楼：[\u202e|\ud83c\udd10-\ud83c\udd89]
        hookBeforeMethod(
            "tbclient.SubPost\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val subPostList = XposedHelpers.getObjectField(param.thisObject, "sub_post_list") as? MutableList<*>
            val pattern = getPattern() ?: return@hookBeforeMethod
            subPostList?.removeIf { o: Any? -> pattern.matcher(parsePbContent(o, "content")).find() }
        }

        // 楼层回复
        hookBeforeMethod(
            "tbclient.PbFloor.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType
        ) { param ->
            val subPostList = XposedHelpers.getObjectField(param.thisObject, "subpost_list") as? MutableList<*>
            val pattern = getPattern() ?: return@hookBeforeMethod
            subPostList?.removeIf { o: Any? -> pattern.matcher(parsePbContent(o, "content")).find() }
        }
    }
}
