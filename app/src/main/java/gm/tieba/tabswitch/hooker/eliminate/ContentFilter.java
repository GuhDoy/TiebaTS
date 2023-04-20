package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.Parser;

public class ContentFilter extends XposedContext implements IHooker, RegexFilter {
    private final Set<Object> mIds = new HashSet<>();

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
                initIdList(param.thisObject, pattern);

                postList.removeIf(o -> ((Integer) XposedHelpers.getObjectField(o, "floor") != 1)
                        && (pattern.matcher(Parser.parsePbContent(o, "content")).find()
                        || mIds.contains(XposedHelpers.getObjectField(o, "author_id"))));
            }
        });
        // 楼中楼：[\u202e|\ud83c\udd10-\ud83c\udd89]
        XposedHelpers.findAndHookMethod("tbclient.SubPost$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var subPostList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "sub_post_list");
                if (subPostList == null) return;
                final var pattern = getPattern();
                subPostList.removeIf(o -> pattern.matcher(Parser.parsePbContent(o, "content")).find()
                        || mIds.contains(XposedHelpers.getObjectField(o, "author_id")));
            }
        });
        // 楼层回复
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var subpostList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "subpost_list");
                if (subpostList == null) return;
                final var pattern = getPattern();
                subpostList.removeIf(o -> {
                    if (pattern.matcher(Parser.parsePbContent(o, "content")).find()) {
                        return true;
                    }

                    final var author = XposedHelpers.getObjectField(o, "author");
                    final var authors = new String[]{(String) XposedHelpers.getObjectField(author, "name"),
                            (String) XposedHelpers.getObjectField(author, "name_show")};
                    return Arrays.stream(authors).anyMatch(s -> pattern.matcher(s).find());
                });
            }
        });
    }

    private void initIdList(final Object thisObject, final Pattern pattern) {
        final var userList = (List<?>) XposedHelpers.getObjectField(thisObject, "user_list");
        for (final var user : userList) {
            final var authors = new String[]{(String) XposedHelpers.getObjectField(user, "name"),
                    (String) XposedHelpers.getObjectField(user, "name_show")};
            if (Arrays.stream(authors).anyMatch(s -> pattern.matcher(s).find())) {
                mIds.add(XposedHelpers.getObjectField(user, "id"));
            }
        }
    }
}
