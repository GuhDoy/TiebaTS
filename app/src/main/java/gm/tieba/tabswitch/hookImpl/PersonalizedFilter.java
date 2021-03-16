package gm.tieba.tabswitch.hookImpl;


import java.util.List;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.util.Reflect;

public class PersonalizedFilter extends Hook {
    public static void hook(ClassLoader classLoader, String personalizedFilter) throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (threadList == null) return;
                label:
                for (int i = 0; i < threadList.size(); i++) {
                    if (Pattern.compile(personalizedFilter).matcher(Reflect.pbContentParser(threadList.get(i), "first_post_content")).find()) {
                        threadList.remove(i);
                        i--;
                        continue;
                    }

                    String[] strings = new String[]{(String) XposedHelpers.getObjectField(threadList.get(i), "title"),
                            (String) XposedHelpers.getObjectField(threadList.get(i), "fname")};
                    for (String string : strings)
                        if (Pattern.compile(personalizedFilter).matcher(string).find()) {
                            threadList.remove(i);
                            i--;
                            continue label;
                        }

                    Object author = XposedHelpers.getObjectField(threadList.get(i), "author");
                    String[] authors = new String[]{(String) XposedHelpers.getObjectField(author, "name"),
                            (String) XposedHelpers.getObjectField(author, "name_show")};
                    for (String string : authors)
                        if (Pattern.compile(personalizedFilter).matcher(string).find()) {
                            threadList.remove(i);
                            i--;
                            break;
                        }
                }
            }
        });
    }
}