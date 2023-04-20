package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.Parser;

public class FrsPageFilter extends XposedContext implements IHooker, RegexFilter {

    @NonNull
    @Override
    public String key() {
        return "frs_page_filter";
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (threadList == null) return;
                final var pattern = getPattern();
                threadList.removeIf(o -> {
                    if (pattern.matcher(Parser.parsePbContent(o, "first_post_content")).find()) {
                        return true;
                    }

                    final var strings = new String[]{(String) XposedHelpers.getObjectField(o, "title"),
                            (String) XposedHelpers.getObjectField(o, "fname")};
                    if (Arrays.stream(strings).anyMatch(s -> pattern.matcher(s).find())) {
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
}
