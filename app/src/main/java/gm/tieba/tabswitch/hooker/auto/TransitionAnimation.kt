package gm.tieba.tabswitch.hooker.auto

import android.app.Activity
import android.os.Build
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.isTbSatisfyVersionRequirement

@Suppress("DEPRECATION")
class TransitionAnimation : XposedContext(), IHooker {
    private lateinit var activityPendingTransitionFactory: Class<*>
    private var CHAT_SQUARE_FADE_IN = 0
    private var CHAT_SQUARE_FADE_OUT = 0
    private var RES_BIG_IMAGE_IN_FROM_RIGHT = 0
    private var RES_BIG_IMAGE_OUT_TO_RIGHT = 0
    private var RES_CUSTOM_FADE_IN = 0
    private var RES_CUSTOM_FADE_OUT = 0
    private var RES_CUSTOM_IN_FROM_RIGHT = 0
    private var RES_CUSTOM_OUT_TO_RIGHT = 0
    private var RES_FADE_OUT = 0
    private var RES_NFADE_IN = 0
    private var RES_NORMAL_IN_FROM_BOTTOM = 0
    private var RES_NORMAL_IN_FROM_LEFT = 0
    private var RES_NORMAL_IN_FROM_RIGHT = 0
    private var RES_NORMAL_OUT_TO_BOTTOM = 0
    private var RES_NORMAL_OUT_TO_LEFT = 0
    private var RES_NORMAL_OUT_TO_RIGHT = 0

    override fun key(): String {
        return "transition_animation"
    }

    override fun hook() {
        if (!(Build.VERSION.SDK_INT >= 34 && isTbSatisfyVersionRequirement("12.58.2.1"))) {
            return
        }

        activityPendingTransitionFactory =
            findClass("com.baidu.tbadk.ActivityPendingTransitionFactory")

        CHAT_SQUARE_FADE_IN = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "CHAT_SQUARE_FADE_IN")
        CHAT_SQUARE_FADE_OUT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "CHAT_SQUARE_FADE_OUT")
        RES_BIG_IMAGE_IN_FROM_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_BIG_IMAGE_IN_FROM_RIGHT")
        RES_BIG_IMAGE_OUT_TO_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_BIG_IMAGE_OUT_TO_RIGHT")
        RES_CUSTOM_FADE_IN = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_CUSTOM_FADE_IN")
        RES_CUSTOM_FADE_OUT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_CUSTOM_FADE_OUT")
        RES_CUSTOM_IN_FROM_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_CUSTOM_IN_FROM_RIGHT")
        RES_CUSTOM_OUT_TO_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_CUSTOM_OUT_TO_RIGHT")
        RES_FADE_OUT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_FADE_OUT")
        RES_NFADE_IN = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NFADE_IN")
        RES_NORMAL_IN_FROM_BOTTOM = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_IN_FROM_BOTTOM")
        RES_NORMAL_IN_FROM_LEFT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_IN_FROM_LEFT")
        RES_NORMAL_IN_FROM_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_IN_FROM_RIGHT")
        RES_NORMAL_OUT_TO_BOTTOM = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_OUT_TO_BOTTOM")
        RES_NORMAL_OUT_TO_LEFT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_OUT_TO_LEFT")
        RES_NORMAL_OUT_TO_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_OUT_TO_RIGHT")

        hookReplaceMethod(
            activityPendingTransitionFactory,
            "enterExitAnimation",
            "com.baidu.tbadk.TbPageContext", Int::class.javaPrimitiveType
        ) { param ->
            enterExitAnimation(param.args[0], param.args[1] as Int)
        }

        hookReplaceMethod(
            activityPendingTransitionFactory,
            "closeAnimation",
            "com.baidu.tbadk.TbPageContext", Int::class.javaPrimitiveType
        ) { param ->
            closeAnimation(param.args[0], param.args[1] as Int)
        }
    }

    private fun enterExitAnimation(tbPageContext: Any, i: Int) {
        var animationType = i
        val pageActivity = XposedHelpers.callMethod(tbPageContext, "getPageActivity") as Activity
        if (XposedHelpers.getStaticBooleanField(activityPendingTransitionFactory, "IS_CUSTOM_FROM_THIRD_PARTY")) {
            animationType = 3
        }
        when (animationType) {
            0 -> pageActivity.overridePendingTransition(0, 0)
            1 -> pageActivity.overridePendingTransition(RES_NORMAL_IN_FROM_RIGHT, RES_FADE_OUT)
            2 -> pageActivity.overridePendingTransition(RES_BIG_IMAGE_IN_FROM_RIGHT, RES_FADE_OUT)
            3 -> pageActivity.overridePendingTransition(RES_CUSTOM_IN_FROM_RIGHT, RES_CUSTOM_FADE_OUT)
            4 -> pageActivity.overridePendingTransition(RES_NORMAL_IN_FROM_BOTTOM, RES_FADE_OUT)
            5 -> pageActivity.overridePendingTransition(CHAT_SQUARE_FADE_IN, CHAT_SQUARE_FADE_OUT)
            6 -> pageActivity.overridePendingTransition(RES_NORMAL_IN_FROM_LEFT, RES_FADE_OUT)
            else -> pageActivity.overridePendingTransition(RES_NORMAL_IN_FROM_RIGHT, RES_FADE_OUT)
        }
    }

    private fun closeAnimation(tbPageContext: Any, i: Int) {
        var animationType = i
        val pageActivity = XposedHelpers.callMethod(tbPageContext, "getPageActivity") as Activity
        if (XposedHelpers.getStaticBooleanField(activityPendingTransitionFactory, "IS_CUSTOM_FROM_THIRD_PARTY")) {
            animationType = 3
        }
        when (animationType) {
            0 -> pageActivity.overridePendingTransition(0, 0)
            1 -> pageActivity.overridePendingTransition(RES_NFADE_IN, RES_NORMAL_OUT_TO_RIGHT)
            2 -> pageActivity.overridePendingTransition(RES_NFADE_IN, RES_BIG_IMAGE_OUT_TO_RIGHT)
            3 -> pageActivity.overridePendingTransition(RES_CUSTOM_FADE_IN, RES_CUSTOM_OUT_TO_RIGHT)
            4 -> pageActivity.overridePendingTransition(RES_NFADE_IN, RES_NORMAL_OUT_TO_BOTTOM)
            5 -> pageActivity.overridePendingTransition(CHAT_SQUARE_FADE_IN, CHAT_SQUARE_FADE_OUT)
            6 -> pageActivity.overridePendingTransition(RES_NFADE_IN, RES_NORMAL_OUT_TO_LEFT)
            else -> pageActivity.overridePendingTransition(RES_NFADE_IN, RES_NORMAL_OUT_TO_RIGHT)
        }
    }
}
