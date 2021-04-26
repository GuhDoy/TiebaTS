package gm.tieba.tabswitch.hooker;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.Hooker;
import gm.tieba.tabswitch.hooker.model.Preferences;
import gm.tieba.tabswitch.util.Reflect;

public class ContentFilter extends BaseHooker implements Hooker {
    private final List<Object> mIdList = new ArrayList<>();

    public void hook() throws Throwable {
        final Pattern pattern = Pattern.compile(Preferences.getString("content_filter"));
        XposedHelpers.findAndHookMethod("tbclient.PbPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> postList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "post_list");
                if (postList == null) return;
                initIdList(param.thisObject, pattern);

                for (int i = 0; i < postList.size(); i++) {
                    if ((int) XposedHelpers.getObjectField(postList.get(i), "floor") == 1) continue;
                    if (pattern.matcher(Reflect.parsePbContent(postList.get(i), "content")).find()) {
                        postList.remove(i);
                        i--;
                        continue;
                    }
                    if (mIdList.contains(XposedHelpers.getObjectField(postList.get(i), "author_id"))) {
                        postList.remove(i);
                        i--;
                    }
                }
            }
        });
        //楼中楼：[\u202e|\ud83c\udd10-\ud83c\udd89]
        XposedHelpers.findAndHookMethod("tbclient.SubPost$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> subPostList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "sub_post_list");
                if (subPostList == null) return;
                for (int i = 0; i < subPostList.size(); i++) {
                    if (pattern.matcher(Reflect.parsePbContent(subPostList.get(i), "content")).find()) {
                        subPostList.remove(i);
                        i--;
                        continue;
                    }
                    if (mIdList.contains(XposedHelpers.getObjectField(subPostList.get(i), "author_id"))) {
                        subPostList.remove(i);
                        i--;
                    }
                }
            }
        });
        //楼层回复
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> subpostList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "subpost_list");
                if (subpostList == null) return;
                for (int i = 0; i < subpostList.size(); i++) {
                    if (pattern.matcher(Reflect.parsePbContent(subpostList.get(i), "content")).find()) {
                        subpostList.remove(i);
                        i--;
                        continue;
                    }

                    Object author = XposedHelpers.getObjectField(subpostList.get(i), "author");
                    String[] authors = new String[]{(String) XposedHelpers.getObjectField(author, "name"),
                            (String) XposedHelpers.getObjectField(author, "name_show")};
                    for (String string : authors) {
                        if (pattern.matcher(string).find()) {
                            subpostList.remove(i);
                            i--;
                            break;
                        }
                    }
                }
            }
        });
    }

    private void initIdList(Object thisObject, Pattern pattern) {
        List<?> userList = (List<?>) XposedHelpers.getObjectField(thisObject, "user_list");
        for (int i = 0; i < userList.size(); i++) {
            String[] authors = new String[]{(String) XposedHelpers.getObjectField(userList.get(i), "name"),
                    (String) XposedHelpers.getObjectField(userList.get(i), "name_show")};
            for (String string : authors) {
                if (pattern.matcher(string).find()) {
                    mIdList.add(XposedHelpers.getObjectField(userList.get(i), "id"));
                    break;
                }
            }
        }
    }
}
