package gm.tieba.tabswitch.hooker.minus;


import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.Parser;

public class PersonalizedFilter extends XposedContext implements IHooker {
    public void hook() throws Throwable {
        final Pattern pattern = Pattern.compile(Preferences.getString("personalized_filter"));
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<?> threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (threadList == null) return;
                threadList.removeIf((Predicate<Object>) o -> {
                    if (pattern.matcher(Parser.parsePbContent(o, "first_post_content")).find()) {
                        return true;
                    }

                    String[] strings = new String[]{(String) XposedHelpers.getObjectField(o, "title"),
                            (String) XposedHelpers.getObjectField(o, "fname")};
                    for (String string : strings) {
                        if (pattern.matcher(string).find()) {
                            return true;
                        }
                    }

                    Object author = XposedHelpers.getObjectField(o, "author");
                    String[] authors = new String[]{(String) XposedHelpers.getObjectField(author, "name"),
                            (String) XposedHelpers.getObjectField(author, "name_show")};
                    for (String string : authors) {
                        if (pattern.matcher(string).find()) {
                            return true;
                        }
                    }
                    return false;
                });
            }
        });
    }
}
