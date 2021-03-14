package gm.tieba.tabswitch.hookImpl;


import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.util.TbProtoParser;

public class PersonalizedFilter extends Hook {
    public static void hook(ClassLoader classLoader, String personalizedFilter) throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field threadList = param.thisObject.getClass().getDeclaredField("thread_list");
                threadList.setAccessible(true);
                List<?> threadInfos = (List<?>) threadList.get(param.thisObject);
                if (threadInfos == null) return;
                label:
                for (int i = 0; i < threadInfos.size(); i++) {
                    if (Pattern.compile(personalizedFilter).matcher(TbProtoParser.pbContentParser(threadInfos.get(i), "first_post_content")).find()) {
                        threadInfos.remove(i);
                        i--;
                        continue;
                    }

                    Field[] fields = new Field[]{threadInfos.get(i).getClass().getDeclaredField("title"),
                            threadInfos.get(i).getClass().getDeclaredField("fname")};
                    for (Field mField : fields) {
                        mField.setAccessible(true);
                        if (Pattern.compile(personalizedFilter).matcher((String) mField.get(threadInfos.get(i))).find()) {
                            threadInfos.remove(i);
                            i--;
                            continue label;
                        }
                    }

                    Field author = threadInfos.get(i).getClass().getDeclaredField("author");
                    author.setAccessible(true);
                    Field[] authors = new Field[]{author.get(threadInfos.get(i)).getClass().getDeclaredField("name"),
                            author.get(threadInfos.get(i)).getClass().getDeclaredField("name_show")};
                    for (Field mField : authors) {
                        mField.setAccessible(true);
                        if (Pattern.compile(personalizedFilter).matcher((String) mField.get(author.get(threadInfos.get(i)))).find()) {
                            threadInfos.remove(i);
                            i--;
                            continue label;
                        }
                    }
                }
            }
        });
    }
}