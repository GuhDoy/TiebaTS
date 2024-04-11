package gm.tieba.tabswitch.hooker.auto;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper;

public class TransitionAnimation extends XposedContext implements IHooker {

    private Class<?> activityPendingTransitionFactory;
    private int CHAT_SQUARE_FADE_IN;
    private int CHAT_SQUARE_FADE_OUT;
    private int RES_BIG_IMAGE_IN_FROM_RIGHT;
    private int RES_BIG_IMAGE_OUT_TO_RIGHT;
    private int RES_CUSTOM_FADE_IN;
    private int RES_CUSTOM_FADE_OUT;
    private int RES_CUSTOM_IN_FROM_RIGHT;
    private int RES_CUSTOM_OUT_TO_RIGHT;
    private int RES_FADE_OUT;
    private int RES_NFADE_IN;
    private int RES_NORMAL_IN_FROM_BOTTOM;
    private int RES_NORMAL_IN_FROM_LEFT;
    private int RES_NORMAL_IN_FROM_RIGHT;
    private int RES_NORMAL_OUT_TO_BOTTOM;
    private int RES_NORMAL_OUT_TO_LEFT;
    private int RES_NORMAL_OUT_TO_RIGHT;

    @NonNull
    @Override
    public String key() {
        return "transition_animation";
    }

    @Override
    public void hook() throws Throwable {
        if (!(Build.VERSION.SDK_INT >= 34 && DeobfuscationHelper.isTbSatisfyVersionRequirement("12.58.2.1", DeobfuscationHelper.getTbVersion(getContext())))) {
            return;
        }

        activityPendingTransitionFactory = XposedHelpers.findClass("com.baidu.tbadk.ActivityPendingTransitionFactory", sClassLoader);

        CHAT_SQUARE_FADE_IN = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "CHAT_SQUARE_FADE_IN");
        CHAT_SQUARE_FADE_OUT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "CHAT_SQUARE_FADE_OUT");
        RES_BIG_IMAGE_IN_FROM_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_BIG_IMAGE_IN_FROM_RIGHT");
        RES_BIG_IMAGE_OUT_TO_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_BIG_IMAGE_OUT_TO_RIGHT");
        RES_CUSTOM_FADE_IN = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_CUSTOM_FADE_IN");
        RES_CUSTOM_FADE_OUT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_CUSTOM_FADE_OUT");
        RES_CUSTOM_IN_FROM_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_CUSTOM_IN_FROM_RIGHT");
        RES_CUSTOM_OUT_TO_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_CUSTOM_OUT_TO_RIGHT");
        RES_FADE_OUT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_FADE_OUT");
        RES_NFADE_IN = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NFADE_IN");
        RES_NORMAL_IN_FROM_BOTTOM = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_IN_FROM_BOTTOM");
        RES_NORMAL_IN_FROM_LEFT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_IN_FROM_LEFT");
        RES_NORMAL_IN_FROM_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_IN_FROM_RIGHT");
        RES_NORMAL_OUT_TO_BOTTOM = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_OUT_TO_BOTTOM");
        RES_NORMAL_OUT_TO_LEFT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_OUT_TO_LEFT");
        RES_NORMAL_OUT_TO_RIGHT = XposedHelpers.getStaticIntField(activityPendingTransitionFactory, "RES_NORMAL_OUT_TO_RIGHT");

        XposedHelpers.findAndHookMethod(
                activityPendingTransitionFactory,
                "enterExitAnimation",
                "com.baidu.tbadk.TbPageContext", int.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        enterExitAnimation(param.args[0], (int) param.args[1]);
                        return null;
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                activityPendingTransitionFactory,
                "closeAnimation",
                "com.baidu.tbadk.TbPageContext", int.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        closeAnimation(param.args[0], (int) param.args[1]);
                        return null;
                    }
                }
        );
    }

    private void enterExitAnimation(Object tbPageContext, int i) {
        Activity pageActivity = (Activity) XposedHelpers.callMethod(tbPageContext, "getPageActivity");
        if (XposedHelpers.getStaticBooleanField(activityPendingTransitionFactory, "IS_CUSTOM_FROM_THIRD_PARTY")) {
            i = 3;
        }
        switch (i) {
            case 0:
                pageActivity.overridePendingTransition(0, 0);
                return;
            case 1:
                pageActivity.overridePendingTransition(RES_NORMAL_IN_FROM_RIGHT, RES_FADE_OUT);
                return;
            case 2:
                pageActivity.overridePendingTransition(RES_BIG_IMAGE_IN_FROM_RIGHT, RES_FADE_OUT);
                return;
            case 3:
                pageActivity.overridePendingTransition(RES_CUSTOM_IN_FROM_RIGHT, RES_CUSTOM_FADE_OUT);
                return;
            case 4:
                pageActivity.overridePendingTransition(RES_NORMAL_IN_FROM_BOTTOM, RES_FADE_OUT);
                return;
            case 5:
                pageActivity.overridePendingTransition(CHAT_SQUARE_FADE_IN, CHAT_SQUARE_FADE_OUT);
                return;
            case 6:
                pageActivity.overridePendingTransition(RES_NORMAL_IN_FROM_LEFT, RES_FADE_OUT);
                return;
            default:
                pageActivity.overridePendingTransition(RES_NORMAL_IN_FROM_RIGHT, RES_FADE_OUT);
                return;
        }
    }

    private void closeAnimation(Object tbPageContext, int i) {
        Activity pageActivity = (Activity) XposedHelpers.callMethod(tbPageContext, "getPageActivity");
        if (XposedHelpers.getStaticBooleanField(activityPendingTransitionFactory, "IS_CUSTOM_FROM_THIRD_PARTY")) {
            i = 3;
        }
        switch (i) {
            case 0:
                pageActivity.overridePendingTransition(0, 0);
                return;
            case 1:
                pageActivity.overridePendingTransition(RES_NFADE_IN, RES_NORMAL_OUT_TO_RIGHT);
                return;
            case 2:
                pageActivity.overridePendingTransition(RES_NFADE_IN, RES_BIG_IMAGE_OUT_TO_RIGHT);
                return;
            case 3:
                pageActivity.overridePendingTransition(RES_CUSTOM_FADE_IN, RES_CUSTOM_OUT_TO_RIGHT);
                return;
            case 4:
                pageActivity.overridePendingTransition(RES_NFADE_IN, RES_NORMAL_OUT_TO_BOTTOM);
                return;
            case 5:
                pageActivity.overridePendingTransition(CHAT_SQUARE_FADE_IN, CHAT_SQUARE_FADE_OUT);
                return;
            case 6:
                pageActivity.overridePendingTransition(RES_NFADE_IN, RES_NORMAL_OUT_TO_LEFT);
                return;
            default:
                pageActivity.overridePendingTransition(RES_NFADE_IN, RES_NORMAL_OUT_TO_RIGHT);
                return;
        }
    }
}
