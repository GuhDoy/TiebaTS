package gm.tieba.tabswitch.hooker;


import java.util.List;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.Hooker;
import gm.tieba.tabswitch.hooker.model.Preferences;
import gm.tieba.tabswitch.util.Reflect;

public class PersonalizedFilter extends BaseHooker implements Hooker {
    public void hook() throws Throwable {
        final Pattern pattern = Pattern.compile(Preferences.getPersonalizedFilter());
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (threadList == null) return;
                label:
                for (int i = 0; i < threadList.size(); i++) {
                    if (pattern.matcher(Reflect.parsePbContent(threadList.get(i), "first_post_content")).find()) {
                        threadList.remove(i);
                        i--;
                        continue;
                    }

                    String[] strings = new String[]{(String) XposedHelpers.getObjectField(threadList.get(i), "title"),
                            (String) XposedHelpers.getObjectField(threadList.get(i), "fname")};
                    for (String string : strings) {
                        if (pattern.matcher(string).find()) {
                            threadList.remove(i);
                            i--;
                            continue label;
                        }
                    }

                    Object author = XposedHelpers.getObjectField(threadList.get(i), "author");
                    String[] authors = new String[]{(String) XposedHelpers.getObjectField(author, "name"),
                            (String) XposedHelpers.getObjectField(author, "name_show")};
                    for (String string : authors) {
                        if (pattern.matcher(string).find()) {
                            threadList.remove(i);
                            i--;
                            break;
                        }
                    }
                }
            }
        });
    }
}
