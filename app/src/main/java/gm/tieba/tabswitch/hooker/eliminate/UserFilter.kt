package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;

public class UserFilter extends XposedContext implements IHooker, RegexFilter {
    private final Set<Object> mIds = new HashSet<>();

    @NonNull
    @Override
    public String key() {
        return "user_filter";
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
                    final var author = XposedHelpers.getObjectField(o, "author");
                    final var authors = new String[]{(String) XposedHelpers.getObjectField(author, "name"),
                            (String) XposedHelpers.getObjectField(author, "name_show")};
                    return Arrays.stream(authors).anyMatch(s -> pattern.matcher(s).find());
                });
            }
        });

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

        // 楼层
        XposedHelpers.findAndHookMethod("tbclient.PbPage.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var postList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "post_list");
                if (postList == null) return;
                final var pattern = getPattern();
                initIdList(param.thisObject, pattern);

                postList.removeIf(o -> ((Integer) XposedHelpers.getObjectField(o, "floor") != 1)
                        && mIds.contains(XposedHelpers.getObjectField(o, "author_id")));
            }
        });
        // 楼中楼：[\u202e|\ud83c\udd10-\ud83c\udd89]
        XposedHelpers.findAndHookMethod("tbclient.SubPost$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var subPostList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "sub_post_list");
                if (subPostList == null) return;
                subPostList.removeIf(o -> mIds.contains(XposedHelpers.getObjectField(o, "author_id")));
            }
        });
        // 楼层回复
        XposedHelpers.findAndHookMethod("tbclient.PbFloor.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var subpostList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "subpost_list");
                if (subpostList == null) return;
                final var pattern = getPattern();
                subpostList.removeIf(o -> {
                    final var author = XposedHelpers.getObjectField(o, "author");
                    final var authors = new String[]{(String) XposedHelpers.getObjectField(author, "name"),
                            (String) XposedHelpers.getObjectField(author, "name_show")};
                    return Arrays.stream(authors).anyMatch(s -> pattern.matcher(s).find());
                });
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

    private void initIdList(final Object thisObject, final Pattern pattern) {
        final var userList = (List<?>) XposedHelpers.getObjectField(thisObject, "user_list");
        for (final var user : userList) {
            final var authors = new String[]{(String) XposedHelpers.getObjectField(user, "name"),
                    (String) XposedHelpers.getObjectField(user, "name_show")};
            if (Arrays.stream(authors).anyMatch(s -> pattern.matcher(s).find())) {
                mIds.add(XposedHelpers.getObjectField(user, "id"));
            }
        }
    }

}
