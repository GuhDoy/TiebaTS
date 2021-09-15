package gm.tieba.tabswitch.hooker.eliminate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.Parser;

public class ContentFilter extends XposedContext implements IHooker {
    private final Set<Object> mIds = new HashSet<>();

    public void hook() throws Throwable {
        final Pattern pattern = Pattern.compile(Preferences.getString("content_filter"));
        // 楼层
        XposedHelpers.findAndHookMethod("tbclient.PbPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> postList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "post_list");
                if (postList == null) return;
                initIdList(param.thisObject, pattern);

                postList.removeIf(o -> ((Integer) XposedHelpers.getObjectField(o, "floor") != 1)
                        && (pattern.matcher(Parser.parsePbContent(o, "content")).find()
                        || mIds.contains(XposedHelpers.getObjectField(o, "author_id"))));
            }
        });
        // 楼中楼：[\u202e|\ud83c\udd10-\ud83c\udd89]
        XposedHelpers.findAndHookMethod("tbclient.SubPost$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> subPostList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "sub_post_list");
                if (subPostList == null) return;
                subPostList.removeIf(o -> pattern.matcher(Parser.parsePbContent(o, "content")).find()
                        || mIds.contains(XposedHelpers.getObjectField(o, "author_id")));
            }
        });
        // 楼层回复
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> subpostList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "subpost_list");
                if (subpostList == null) return;
                subpostList.removeIf(o -> {
                    if (pattern.matcher(Parser.parsePbContent(o, "content")).find()) {
                        return true;
                    }

                    Object author = XposedHelpers.getObjectField(o, "author");
                    String[] authors = new String[]{(String) XposedHelpers.getObjectField(author, "name"),
                            (String) XposedHelpers.getObjectField(author, "name_show")};
                    return Arrays.stream(authors).anyMatch(s -> pattern.matcher(s).find());
                });
            }
        });
    }

    private void initIdList(Object thisObject, Pattern pattern) {
        List<?> userList = (List<?>) XposedHelpers.getObjectField(thisObject, "user_list");
        for (int i = 0; i < userList.size(); i++) {
            String[] authors = new String[]{(String) XposedHelpers.getObjectField(userList.get(i), "name"),
                    (String) XposedHelpers.getObjectField(userList.get(i), "name_show")};
            if (Arrays.stream(authors).anyMatch(s -> pattern.matcher(s).find())) {
                mIds.add(XposedHelpers.getObjectField(userList.get(i), "id"));
            }
        }
    }
}
