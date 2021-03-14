package gm.tieba.tabswitch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hookImpl.AutoSign;
import gm.tieba.tabswitch.hookImpl.ContentFilter;
import gm.tieba.tabswitch.hookImpl.CreateView;
import gm.tieba.tabswitch.hookImpl.EyeshieldMode;
import gm.tieba.tabswitch.hookImpl.HistoryCache;
import gm.tieba.tabswitch.hookImpl.HomeRecommend;
import gm.tieba.tabswitch.hookImpl.OriginSrc;
import gm.tieba.tabswitch.hookImpl.Purify;
import gm.tieba.tabswitch.hookImpl.PurifyEnter;
import gm.tieba.tabswitch.hookImpl.PurifyMy;
import gm.tieba.tabswitch.hookImpl.RedTip;
import gm.tieba.tabswitch.hookImpl.SaveImages;
import gm.tieba.tabswitch.hookImpl.StorageRedirect;
import gm.tieba.tabswitch.hookImpl.ThreadStore;
import gm.tieba.tabswitch.util.TbProtoParser;

public class HookDispatcher extends Hook {
    public static void hook(ClassLoader classLoader, Map.Entry<String, ?> entry, Context context) throws Throwable {
        try {
            switch (entry.getKey()) {
                case "home_recommend":
                    if ((Boolean) entry.getValue()) HomeRecommend.hook(classLoader);
                    break;
                case "enter_forum":
                    if (!(Boolean) entry.getValue()) break;
                    try {
                        XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterEnterForumDelegateStatic", classLoader, "createFragmentTabStructure", XC_MethodReplacement.returnConstant(null));
                    } catch (XposedHelpers.ClassNotFoundError ignored) {
                    }
                    XposedHelpers.findAndHookMethod("com.baidu.tieba.enterForum.home.EnterForumDelegateStatic", classLoader, "isAvailable", XC_MethodReplacement.returnConstant(false));
                    break;
                case "new_category":
                    if ((Boolean) entry.getValue())
                        XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterNewCategoryDelegateStatic", classLoader, "isAvailable", XC_MethodReplacement.returnConstant(false));
                    break;
                case "my_message":
                    if ((Boolean) entry.getValue())
                        XposedHelpers.findAndHookMethod("com.baidu.tieba.imMessageCenter.im.chat.notify.ImMessageCenterDelegateStatic", classLoader, "isAvailable", XC_MethodReplacement.returnConstant(false));
                    break;
                case "mine":
                    if ((Boolean) entry.getValue())
                        XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterDelegateStatic", classLoader, "isAvailable", XC_MethodReplacement.returnConstant(false));
                    break;
                case "purify":
                    if ((Boolean) entry.getValue()) Purify.hook(classLoader);
                    break;
                case "purify_enter":
                    if ((Boolean) entry.getValue()) PurifyEnter.hook(classLoader);
                    break;
                case "purify_my":
                    if ((Boolean) entry.getValue()) PurifyMy.hook(classLoader);
                    break;
                case "red_tip":
                    if ((Boolean) entry.getValue()) RedTip.hook(classLoader);
                    break;
                case "follow_filter":
                    if (!(Boolean) entry.getValue()) break;
                    XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
                        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (Hook.follow == null) return;
                            Field field = param.thisObject.getClass().getDeclaredField("thread_list");
                            field.setAccessible(true);
                            List<?> list = (List<?>) field.get(param.thisObject);
                            if (list == null) return;
                            for (int i = 0; i < list.size(); i++) {
                                boolean isRemove = true;
                                for (String pb : Hook.follow)
                                    if (list.get(i).toString().contains(", fname=" + pb + ", forum_info=")) {
                                        isRemove = false;
                                        break;
                                    }
                                if (isRemove) {
                                    list.remove(i);
                                    i--;
                                }
                            }
                        }
                    });
                    break;
                case "personalized_filter":
                    String personalizedFilter = (String) entry.getValue();
                    if (personalizedFilter == null) break;
                    XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
                        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Field field = param.thisObject.getClass().getDeclaredField("thread_list");
                            field.setAccessible(true);
                            List<?> list = (List<?>) field.get(param.thisObject);
                            if (list == null) return;
                            for (int i = 0; i < list.size(); i++)
                                try {
                                    TbProtoParser.ThreadInfoParser threadInfo = new TbProtoParser.ThreadInfoParser(list.get(i).toString());
                                    Field[] fields = threadInfo.getClass().getDeclaredFields();
                                    for (Field mField : fields)
                                        if (Pattern.compile(personalizedFilter).matcher((String) mField.get(threadInfo)).find() && !mField.getName().equals("tid")) {
                                            list.remove(i);
                                            i--;
                                            break;
                                        }
                                } catch (StringIndexOutOfBoundsException ignored) {
                                }
                        }
                    });
                    break;
                case "content_filter":
                    String contentFilter = (String) entry.getValue();
                    if (contentFilter != null) ContentFilter.hook(classLoader, contentFilter);
                    break;
                case "create_view":
                    if ((Boolean) entry.getValue()) CreateView.hook(classLoader);
                    break;
                case "save_images":
                    if ((Boolean) entry.getValue()) SaveImages.hook(classLoader);
                    break;
                case "thread_store":
                    if ((Boolean) entry.getValue()) ThreadStore.hook(classLoader);
                    break;
                case "history_cache":
                    if ((Boolean) entry.getValue()) HistoryCache.hook(classLoader);
                    break;
                case "auto_sign":
                    if ((Boolean) entry.getValue()) AutoSign.hook(classLoader);
                    break;
                case "open_sign":
                    if (!(Boolean) entry.getValue()) break;
                    XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Activity activity = (Activity) param.thisObject;
                            SharedPreferences sp = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                            if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) != sp.getInt("sign_date", 0) && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) != 0) {
                                Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.signall.SignAllForumActivity");
                                activity.startActivity(intent);
                            }
                        }
                    });
                    XposedHelpers.findAndHookMethod("com.baidu.tieba.signall.SignAllForumActivity", classLoader, "onClick", View.class, new XC_MethodHook() {
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Activity activity = (Activity) param.thisObject;
                            SharedPreferences sp = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                            if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) != sp.getInt("sign_date", 0) && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) != 0) {
                                SharedPreferences.Editor editor = sp.edit();
                                editor.putInt("sign_date", Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
                                editor.apply();
                                activity.finish();
                            }
                        }
                    });
                    break;
                case "origin_src":
                    if ((Boolean) entry.getValue()) OriginSrc.hook(classLoader);
                    break;
                case "storage_redirect":
                    if ((Boolean) entry.getValue()) StorageRedirect.hook(classLoader, context);
                    break;
                case "font_size":
                    if (!(Boolean) entry.getValue()) break;
                    for (int i = 0; i < ruleMapList.size(); i++) {
                        Map<String, String> map = ruleMapList.get(i);
                        if (Objects.equals(map.get("rule"), "Lcom/baidu/tieba/R$id;->new_pb_list:I"))
                            XposedBridge.hookAllConstructors(XposedHelpers.findClass(map.get("class"), classLoader), new XC_MethodHook() {
                                @SuppressLint("ClickableViewAccessibility")
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    Field[] fields = param.thisObject.getClass().getDeclaredFields();
                                    for (Field field : fields) {
                                        field.setAccessible(true);
                                        if (field.get(param.thisObject) instanceof RelativeLayout) {
                                            RelativeLayout relativeLayout = (RelativeLayout) field.get(param.thisObject);
                                            ListView listView = relativeLayout.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("new_pb_list").getInt(null));
                                            if (listView == null) continue;
                                            listView.setOnTouchListener((v, event) -> false);
                                            return;
                                        }
                                    }
                                }
                            });
                    }
                    XposedHelpers.findAndHookMethod("com.baidu.tieba.pb.videopb.fragment.DetailInfoAndReplyFragment", classLoader, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
                        @SuppressLint("ClickableViewAccessibility")
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Field[] fields = param.thisObject.getClass().getDeclaredFields();
                            for (Field field : fields) {
                                field.setAccessible(true);
                                if (field.get(param.thisObject).getClass().getName().equals("com.baidu.adp.widget.ListView.BdTypeRecyclerView")) {
                                    ViewGroup recyclerView = (ViewGroup) field.get(param.thisObject);
                                    recyclerView.setOnTouchListener((v, event) -> false);
                                    return;
                                }
                            }
                        }
                    });
                    break;
                case "eyeshield_mode":
                    if ((Boolean) entry.getValue()) EyeshieldMode.hook(classLoader, context);
                    break;
                case "agree_num":
                    if (!(Boolean) entry.getValue()) break;
                    XposedHelpers.findAndHookMethod("tbclient.Agree$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
                        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Field diffAgreeNum = param.thisObject.getClass().getDeclaredField("diff_agree_num");
                            diffAgreeNum.setAccessible(true);
                            Field agreeNum = param.thisObject.getClass().getDeclaredField("agree_num");
                            agreeNum.setAccessible(true);
                            agreeNum.set(param.thisObject, diffAgreeNum.get(param.thisObject));
                        }
                    });
                    break;
                case "frs_tab":
                    if (!(Boolean) entry.getValue()) break;
                    XposedHelpers.findAndHookMethod("tbclient.FrsPage.NavTabInfo$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
                        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Field field = param.thisObject.getClass().getDeclaredField("tab");
                            field.setAccessible(true);
                            List<?> list = (List<?>) field.get(param.thisObject);
                            if (list == null) return;
                            for (int i = 0; i < list.size(); i++) {
                                Field tabType = list.get(i).getClass().getDeclaredField("tab_type");
                                tabType.setAccessible(true);
                                if ((int) tabType.get(list.get(i)) == 13) {
                                    Collections.swap(list, i, i + 1);
                                    return;
                                }
                            }
                        }
                    });
                    break;
            }
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log(e);
        }
    }
}