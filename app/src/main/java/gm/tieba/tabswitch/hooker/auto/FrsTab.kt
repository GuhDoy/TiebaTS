package gm.tieba.tabswitch.hooker.auto

import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher
import java.lang.reflect.Method

class FrsTab : XposedContext(), IHooker, Obfuscated {
    override fun key(): String {
        return "frs_tab"
    }

    override fun matchers(): List<Matcher> {
        return listOf(
            StringMatcher("forum_tab_current_list"),
            StringMatcher("c/f/frs/page?cmd=301001&format=protobuf")
        )
    }

    private var mPosition = 0

    override fun hook() {
        hookBeforeMethod(
            "tbclient.FrsPage.DataRes\$Builder",
            "build", Boolean::class.javaPrimitiveType,
        ) { param ->
            val list = XposedHelpers.getObjectField(param.thisObject, "frs_main_tab_list") as? List<*>
            list?.forEachIndexed { index, item ->
                if (XposedHelpers.getObjectField(item, "tab_type") as Int == 14) {
                    mPosition = index
                    XposedHelpers.setObjectField(
                        param.thisObject,
                        "frs_tab_default",
                        XposedHelpers.getObjectField(item, "tab_id") as Int
                    )
                    return@hookBeforeMethod
                }
            }
        }

        findRule(matchers()) { matcher, clazz, method ->
            when (matcher) {
                "forum_tab_current_list" -> {
                    if ("com.baidu.tieba.forum.controller.TopController" != clazz) return@findRule
                    val targetMethod: Method = try {
                        XposedHelpers.findMethodBestMatch(
                            findClass(clazz),
                            method,
                            null,
                            findClass(clazz)
                        )
                    } catch (e: NoSuchMethodError) { // 12.57+
                        return@findRule
                    }

                    hookAfterMethod(targetMethod) { param ->
                        val viewPager: Any? = try {
                            XposedHelpers.findFirstFieldByExactType(
                                param.args[1].javaClass,
                                findClass("com.baidu.tbadk.widget.CustomViewPager")
                            )[param.args[1]]
                        } catch (e: NoSuchFieldError) {  // 12.56+
                            XposedHelpers.findFirstFieldByExactType(
                                param.args[1].javaClass,
                                findClass("androidx.viewpager.widget.ViewPager")
                            )[param.args[1]]
                        }
                        XposedHelpers.callMethod(viewPager, "setCurrentItem", mPosition)
                    }
                }

                "c/f/frs/page?cmd=301001&format=protobuf" ->  {
                    hookBeforeMethod(
                        clazz, method, "com.baidu.tieba.forum.model.FrsPageRequestMessage"
                    ) { param ->
                        if (XposedHelpers.getObjectField(param.args[0], "sortType") as Int == -1) {
                            val sharedPrefHelper = XposedHelpers.callStaticMethod(
                                findClass("com.baidu.tbadk.core.sharedPref.SharedPrefHelper"),
                                "getInstance"
                            )
                            val lastSortType = XposedHelpers.callMethod(sharedPrefHelper, "getInt", "key_forum_last_sort_type", 0) as Int
                            XposedHelpers.setObjectField(param.args[0], "sortType", lastSortType)
                        }
                    }
                }
            }
        }
    }
}
