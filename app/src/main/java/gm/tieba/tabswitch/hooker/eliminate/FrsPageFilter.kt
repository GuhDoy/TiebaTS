package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;

public class FrsPageFilter extends XposedContext implements IHooker, RegexFilter {

    @NonNull
    @Override
    public String key() {
        return "frs_page_filter";
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.PageData$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                filterPageData(param.thisObject);
            }
        });

        XposedHelpers.findAndHookMethod("tbclient.ThreadList.PageData$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                filterPageData(param.thisObject);
            }
        });
    }

    private void filterPageData(Object pageData) {
        List<?> feedList = (List<?>) XposedHelpers.getObjectField(pageData, "feed_list");
        if (feedList == null) return;
        final var pattern = getPattern();
        feedList.removeIf(
                o -> {
                    Object currFeed = XposedHelpers.getObjectField(o, "feed");
                    if (currFeed != null) {
                        List<?> businessInfo = (List<?>) XposedHelpers.getObjectField(currFeed, "business_info");
                        for (var feedKV : businessInfo) {
                            String currKey = XposedHelpers.getObjectField(feedKV, "key").toString();
                            if (currKey.equals("title") || currKey.equals("abstract")) {
                                String str = XposedHelpers.getObjectField(feedKV, "value").toString();
                                if (pattern.matcher(str).find()) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }
        );
    }
}
