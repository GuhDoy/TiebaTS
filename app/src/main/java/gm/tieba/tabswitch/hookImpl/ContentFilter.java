package gm.tieba.tabswitch.hookImpl;


import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.util.TbProtoParser;

public class ContentFilter extends Hook {
    public static void hook(ClassLoader classLoader, String contentFilter) throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.PbPage.DataRes$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field postList = param.thisObject.getClass().getDeclaredField("post_list");
                postList.setAccessible(true);
                List<?> posts = (List<?>) postList.get(param.thisObject);
                if (posts == null) return;
                for (int i = 0; i < posts.size(); i++) {
                    Field floor = posts.get(i).getClass().getDeclaredField("floor");
                    floor.setAccessible(true);
                    if ((int) floor.get(posts.get(i)) == 1) continue;

                    if (Pattern.compile(contentFilter).matcher(TbProtoParser.pbContentParser(posts.get(i), "content")).find()) {
                        posts.remove(i);
                        i--;
                    }
                }
            }
        });
        //楼中楼：[\u202e|\ud83c\udd10-\ud83c\udd89]
        XposedHelpers.findAndHookMethod("tbclient.SubPost$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field subPostList = param.thisObject.getClass().getDeclaredField("sub_post_list");
                subPostList.setAccessible(true);
                List<?> subPostLists = (List<?>) subPostList.get(param.thisObject);
                if (subPostLists == null) return;
                for (int i = 0; i < subPostLists.size(); i++)
                    if (Pattern.compile(contentFilter).matcher(TbProtoParser.pbContentParser(subPostLists.get(i), "content")).find()) {
                        subPostLists.remove(i);
                        i--;
                    }
            }
        });
        //楼层回复
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field subpostList = param.thisObject.getClass().getDeclaredField("subpost_list");
                subpostList.setAccessible(true);
                List<?> subPostLists = (List<?>) subpostList.get(param.thisObject);
                if (subPostLists == null) return;
                for (int i = 0; i < subPostLists.size(); i++) {
                    if (Pattern.compile(contentFilter).matcher(TbProtoParser.pbContentParser(subPostLists.get(i), "content")).find()) {
                        subPostLists.remove(i);
                        i--;
                        continue;
                    }

                    Field author = subPostLists.get(i).getClass().getDeclaredField("author");
                    author.setAccessible(true);
                    Field[] mFields = new Field[]{author.get(subPostLists.get(i)).getClass().getDeclaredField("name"),
                            author.get(subPostLists.get(i)).getClass().getDeclaredField("name_show")};
                    for (Field mField : mFields) {
                        mField.setAccessible(true);
                        if (Pattern.compile(contentFilter).matcher((String) mField.get(author.get(subPostLists.get(i)))).find()) {
                            subPostLists.remove(i);
                            i--;
                        }
                    }
                }
            }
        });
    }
}