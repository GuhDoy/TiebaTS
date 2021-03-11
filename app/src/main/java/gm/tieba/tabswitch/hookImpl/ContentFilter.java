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
                Field field = param.thisObject.getClass().getDeclaredField("post_list");
                field.setAccessible(true);
                List<?> list = (List<?>) field.get(param.thisObject);
                if (list == null) return;
                for (int i = 0; i < list.size(); i++)
                    try {
                        String post = list.get(i).toString();
                        post = post.substring(post.indexOf(", text=") + 7, post.indexOf(", topic_special_icon="));
                        if (Pattern.compile(contentFilter).matcher(post).find()) {
                            list.remove(i);
                            i--;
                        }
                    } catch (StringIndexOutOfBoundsException ignored) {
                    }
            }
        });
        //楼中楼：[\u202e|\ud83c\udd10-\ud83c\udd89]
        XposedHelpers.findAndHookMethod("tbclient.SubPost$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field field = param.thisObject.getClass().getDeclaredField("sub_post_list");
                field.setAccessible(true);
                List<?> list = (List<?>) field.get(param.thisObject);
                if (list == null) return;
                for (int i = 1; i < list.size(); i++)
                    try {
                        TbProtoParser.SubPostParser subPost = new TbProtoParser.SubPostParser(list.get(i).toString());
                        if (Pattern.compile(contentFilter).matcher(subPost.pbContent).find()) {
                            list.remove(i);
                            i--;
                        }
                    } catch (StringIndexOutOfBoundsException ignored) {
                    }
            }
        });
        //楼层回复
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field field = param.thisObject.getClass().getDeclaredField("subpost_list");
                field.setAccessible(true);
                List<?> list = (List<?>) field.get(param.thisObject);
                if (list == null) return;
                for (int i = 0; i < list.size(); i++)
                    try {
                        TbProtoParser.SubPostParser subPost = new TbProtoParser.SubPostParser(list.get(i).toString());
                        Field[] fields = subPost.getClass().getDeclaredFields();
                        for (Field mField : fields)
                            if (Pattern.compile(contentFilter).matcher((String) mField.get(subPost)).find()) {
                                list.remove(i);
                                i--;
                                break;
                            }
                    } catch (StringIndexOutOfBoundsException ignored) {
                    }
            }
        });
    }
}