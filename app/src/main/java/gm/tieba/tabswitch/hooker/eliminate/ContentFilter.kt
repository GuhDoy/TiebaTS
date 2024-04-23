package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.Parser;

public class ContentFilter extends XposedContext implements IHooker, RegexFilter {

    @NonNull
    @Override
    public String key() {
        return "content_filter";
    }

    @Override
    public void hook() throws Throwable {
        // 楼层
        XposedHelpers.findAndHookMethod("tbclient.PbPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var postList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "post_list");
                if (postList == null) return;
                final var pattern = getPattern();

                postList.removeIf(o -> ((Integer) XposedHelpers.getObjectField(o, "floor") != 1)
                        && (pattern.matcher(Parser.parsePbContent(o, "content")).find()));
            }
        });
        // 楼中楼：[\u202e|\ud83c\udd10-\ud83c\udd89]
        XposedHelpers.findAndHookMethod("tbclient.SubPost$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var subPostList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "sub_post_list");
                if (subPostList == null) return;
                final var pattern = getPattern();
                subPostList.removeIf(o -> pattern.matcher(Parser.parsePbContent(o, "content")).find());
            }
        });
        // 楼层回复
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var subpostList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "subpost_list");
                if (subpostList == null) return;
                final var pattern = getPattern();
                subpostList.removeIf(o -> pattern.matcher(Parser.parsePbContent(o, "content")).find());
            }
        });
    }
}
