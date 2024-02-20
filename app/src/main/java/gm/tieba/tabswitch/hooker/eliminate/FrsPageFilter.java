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
        // thread_list is deprecated
//        XposedHelpers.findAndHookMethod("tbclient.FrsPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
//                final var threadList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
//                if (threadList == null) return;
//                final var pattern = getPattern();
//                threadList.removeIf(o -> {
//                    if (pattern.matcher(Parser.parsePbContent(o, "first_post_content")).find()) {
//                        return true;
//                    }
//
//                    final var strings = new String[]{(String) XposedHelpers.getObjectField(o, "title"),
//                            (String) XposedHelpers.getObjectField(o, "fname")};
//                    if (Arrays.stream(strings).anyMatch(s -> pattern.matcher(s).find())) {
//                        return true;
//                    }
//
//                    final var author = XposedHelpers.getObjectField(o, "author");
//                    final var authors = new String[]{(String) XposedHelpers.getObjectField(author, "name"),
//                            (String) XposedHelpers.getObjectField(author, "name_show")};
//                    return Arrays.stream(authors).anyMatch(s -> pattern.matcher(s).find());
//                });
//            }
//        });
        XposedHelpers.findAndHookMethod("tbclient.FrsPage.PageData$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                List<?> feedList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "feed_list");
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

                                List<?> components = (List<?>) XposedHelpers.getObjectField(currFeed, "components");
                                if (components != null ){
                                    for (var component: components) {
                                        if (XposedHelpers.getObjectField(component, "component").toString().equals("feed_head")) {
                                            Object feedHead = XposedHelpers.getObjectField(component, "feed_head");
                                            List<?> mainData = (List<?>) XposedHelpers.getObjectField(feedHead, "main_data");
                                            if (mainData != null) {
                                                for (var feedHeadSymbol: mainData) {
                                                    Object feedHeadText = XposedHelpers.getObjectField(feedHeadSymbol, "text");
                                                    if (feedHeadText != null) {
                                                        String username = (String) XposedHelpers.getObjectField(feedHeadText, "text");
                                                        if (username != null) {
                                                            if (pattern.matcher(username).find()) {
                                                                return true;
                                                            }
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            return false;
                        }
                );
            }
        });
    }
}
