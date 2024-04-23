package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.Parser;

public class PersonalizedFilter extends XposedContext implements IHooker, RegexFilter {

    @NonNull
    @Override
    public String key() {
        return "personalized_filter";
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (threadList == null) return;
                final var pattern = getPattern();
                threadList.removeIf(o -> {
                    if (pattern.matcher(Parser.parsePbContent(o, "first_post_content")).find()) {
                        return true;
                    }

                    return pattern.matcher((String) XposedHelpers.getObjectField(o, "title")).find();
                });
            }
        });
    }
}
