package gm.tieba.tabswitch.widget;

import android.content.Context;

import androidx.annotation.MainThread;

import java.util.List;

import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;

public class TbToast extends XposedContext implements Obfuscated {
    public static int LENGTH_SHORT = 2000;
    public static int LENGTH_LONG = 3500;

    @Override
    public List<? extends Matcher> matchers() {
        // setToastString()
        return List.of(new StringMatcher("can not be call not thread! trace = "));
    }

    @MainThread
    public static void showTbToast(String text, int duration) {
        AcRules.findRule(new TbToast().matchers(), (matcher, clazz, method) -> {
            var md = ReflectUtils.findFirstMethodByExactType(clazz, Context.class, String.class, int.class);
            runOnUiThread(() -> ReflectUtils.callStaticMethod(md, getContext(), text, duration));
        });
    }
}
