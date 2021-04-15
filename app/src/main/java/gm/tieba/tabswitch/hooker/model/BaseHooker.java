package gm.tieba.tabswitch.hooker.model;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.AutoSign;
import gm.tieba.tabswitch.hooker.ContentFilter;
import gm.tieba.tabswitch.hooker.CreateView;
import gm.tieba.tabswitch.hooker.EyeshieldMode;
import gm.tieba.tabswitch.hooker.ForbidGesture;
import gm.tieba.tabswitch.hooker.HistoryCache;
import gm.tieba.tabswitch.hooker.HomeRecommend;
import gm.tieba.tabswitch.hooker.MyAttention;
import gm.tieba.tabswitch.hooker.NewSub;
import gm.tieba.tabswitch.hooker.OriginSrc;
import gm.tieba.tabswitch.hooker.PersonalizedFilter;
import gm.tieba.tabswitch.hooker.Purify;
import gm.tieba.tabswitch.hooker.PurifyEnter;
import gm.tieba.tabswitch.hooker.PurifyMy;
import gm.tieba.tabswitch.hooker.RedTip;
import gm.tieba.tabswitch.hooker.SaveImages;
import gm.tieba.tabswitch.hooker.StorageRedirect;
import gm.tieba.tabswitch.hooker.ThreadStore;

public class BaseHooker {
    protected static ClassLoader sClassLoader;
    protected static WeakReference<Context> sContextRef;
    protected static Resources sRes;

    protected BaseHooker() {
    }

    protected BaseHooker(ClassLoader classLoader, Resources res) {
        sClassLoader = classLoader;
        sRes = res;
    }

    public static void init(ClassLoader classLoader, Context context, Resources res, Map.Entry<String, ?> entry) throws Throwable {
        sClassLoader = classLoader;
        sContextRef = new WeakReference<>(context);
        sRes = res;
        switch (entry.getKey()) {
            case "home_recommend":
                if ((Boolean) entry.getValue()) new HomeRecommend().hook();
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
                if (!(Boolean) entry.getValue()) break;
                XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterNewCategoryDelegateStatic", classLoader, "isAvailable", XC_MethodReplacement.returnConstant(false));
                break;
            case "my_message":
                if (!(Boolean) entry.getValue()) break;
                XposedHelpers.findAndHookMethod("com.baidu.tieba.imMessageCenter.im.chat.notify.ImMessageCenterDelegateStatic", classLoader, "isAvailable", XC_MethodReplacement.returnConstant(false));
                break;
            case "mine":
                if (!(Boolean) entry.getValue()) break;
                XposedHelpers.findAndHookMethod("com.baidu.tieba.flutter.view.FlutterDelegateStatic", classLoader, "isAvailable", XC_MethodReplacement.returnConstant(false));
                break;
            case "purify":
                if ((Boolean) entry.getValue()) new Purify().hook();
                break;
            case "purify_enter":
                if ((Boolean) entry.getValue()) new PurifyEnter().hook();
                break;
            case "purify_my":
                if ((Boolean) entry.getValue()) new PurifyMy().hook();
                break;
            case "red_tip":
                if ((Boolean) entry.getValue()) new RedTip().hook();
                break;
            case "follow_filter":
                if (!(Boolean) entry.getValue()) break;
                XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
                    @Override
                    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (Preferences.getFollow() == null) {
                            Looper.prepare();
                            Toast.makeText(context, "暂未获取到关注列表", Toast.LENGTH_LONG).show();
                            Looper.loop();
                        }
                        List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                        if (list == null) return;
                        for (int i = 0; i < list.size(); i++) {
                            if (!Preferences.getFollow().contains(XposedHelpers.getObjectField(list.get(i), "fname"))) {
                                list.remove(i);
                                i--;
                            }
                        }
                    }
                });
                break;
            case "personalized_filter":
                if (Preferences.getPersonalizedFilter() != null)
                    new PersonalizedFilter().hook();
                break;
            case "content_filter":
                if (Preferences.getContentFilter() != null) new ContentFilter().hook();
                break;
            case "create_view":
                if ((Boolean) entry.getValue()) new CreateView().hook();
                break;
            case "thread_store":
                if ((Boolean) entry.getValue()) new ThreadStore().hook();
                break;
            case "history_cache":
                if ((Boolean) entry.getValue()) new HistoryCache().hook();
                break;
            case "new_sub":
                if ((Boolean) entry.getValue()) new NewSub().hook();
                break;
            case "save_images":
                if ((Boolean) entry.getValue()) new SaveImages().hook();
                break;
            case "my_attention":
                if ((Boolean) entry.getValue()) new MyAttention().hook();
                break;
            case "auto_sign":
                if ((Boolean) entry.getValue()) new AutoSign().hook();
                break;
            case "open_sign":
                if (!(Boolean) entry.getValue()) break;
                XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        if (!Preferences.getIsSigned() && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) != 0) {
                            Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.signall.SignAllForumActivity");
                            activity.startActivity(intent);
                        }
                    }
                });
                XposedHelpers.findAndHookMethod("com.baidu.tieba.signall.SignAllForumActivity", classLoader, "onClick", View.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        if (!Preferences.getIsSigned() && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) != 0) {
                            Preferences.putSignDate();
                            activity.finish();
                        }
                    }
                });
                break;
            case "origin_src":
                if ((Boolean) entry.getValue()) new OriginSrc().hook();
                break;
            case "storage_redirect":
                if ((Boolean) entry.getValue()) new StorageRedirect().hook();
                break;
            case "forbid_gesture":
                if ((Boolean) entry.getValue()) new ForbidGesture().hook();
                break;
            case "eyeshield_mode":
                if ((Boolean) entry.getValue()) new EyeshieldMode().hook();
                break;
            case "agree_num":
                if (!(Boolean) entry.getValue()) break;
                XposedHelpers.findAndHookMethod("tbclient.Agree$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
                    @Override
                    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField(param.thisObject, "agree_num", XposedHelpers.getObjectField(param.thisObject, "diff_agree_num"));
                    }
                });
                break;
            case "frs_tab":
                if (!(Boolean) entry.getValue()) break;
                XposedHelpers.findAndHookMethod("tbclient.FrsPage.NavTabInfo$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
                    @Override
                    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "tab");
                        if (list == null) return;
                        for (int i = 0; i < list.size(); i++) {
                            if ((int) XposedHelpers.getObjectField(list.get(i), "tab_type") == 13) {
                                Collections.swap(list, i, i + 1);
                                return;
                            }
                        }
                    }
                });
                break;
        }
    }
}