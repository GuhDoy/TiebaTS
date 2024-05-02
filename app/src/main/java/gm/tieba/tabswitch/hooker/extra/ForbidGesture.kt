package gm.tieba.tabswitch.hooker.extra

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.ResMatcher
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher
import gm.tieba.tabswitch.util.getObjectField
import gm.tieba.tabswitch.util.getR
import org.luckypray.dexkit.query.matchers.ClassMatcher

class ForbidGesture : XposedContext(), IHooker, Obfuscated {
    override fun key(): String {
        return "forbid_gesture"
    }

    override fun matchers(): List<Matcher> {
        return listOf(
            ResMatcher(getR("drawable", "icon_word_t_size").toLong(), "forbid_gesture"),
            SmaliMatcher("Ljava/lang/Math;->sqrt(D)D").apply {
                classMatcher = ClassMatcher.create().className("com.baidu.tbadk.widget.DragImageView")
            }
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    @Throws(Throwable::class)
    override fun hook() {
        // 帖子字号
        findRule(matchers()) { matcher, clazz, method ->
            when (matcher) {
                "forbid_gesture" -> hookReplaceMethod(clazz, method) { null }
                "Ljava/lang/Math;->sqrt(D)D" -> hookAfterMethod(clazz, method, Bitmap::class.java) { param ->
                    param.result = 3 * param.result as Float
                }
            }
        }

        // 视频帖字号
        hookAfterMethod("com.baidu.tieba.pb.videopb.fragment.DetailInfoAndReplyFragment",
            "onCreateView", LayoutInflater::class.java, ViewGroup::class.java, Bundle::class.java) { param ->
            val recyclerView = getObjectField(
                param.thisObject,
                "com.baidu.adp.widget.ListView.BdTypeRecyclerView"
            ) as? ViewGroup
            recyclerView?.setOnTouchListener { _, _ -> false }
        }

        // 帖子进吧
        hookBeforeMethod(
            "com.baidu.tieba.pb.pb.main.PbLandscapeListView",
            "dispatchTouchEvent", MotionEvent::class.java
        ) { param ->
            XposedHelpers.callMethod(param.thisObject, "setForbidDragListener", true)
        }
    }
}
