package gm.tieba.tabswitch;

import android.content.Context;
import android.content.res.Resources;

import java.lang.ref.WeakReference;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.add.CreateView;
import gm.tieba.tabswitch.hooker.add.HistoryCache;
import gm.tieba.tabswitch.hooker.add.MyAttention;
import gm.tieba.tabswitch.hooker.add.NewSub;
import gm.tieba.tabswitch.hooker.add.Ripple;
import gm.tieba.tabswitch.hooker.add.SaveImages;
import gm.tieba.tabswitch.hooker.add.ThreadStore;
import gm.tieba.tabswitch.hooker.auto.AutoSign;
import gm.tieba.tabswitch.hooker.auto.EyeshieldMode;
import gm.tieba.tabswitch.hooker.auto.OpenSign;
import gm.tieba.tabswitch.hooker.auto.OriginSrc;
import gm.tieba.tabswitch.hooker.extra.ForbidGesture;
import gm.tieba.tabswitch.hooker.auto.FrsTab;
import gm.tieba.tabswitch.hooker.extra.Hide;
import gm.tieba.tabswitch.hooker.extra.RedirectImage;
import gm.tieba.tabswitch.hooker.extra.StackTrace;
import gm.tieba.tabswitch.hooker.minus.ContentFilter;
import gm.tieba.tabswitch.hooker.minus.FollowFilter;
import gm.tieba.tabswitch.hooker.minus.FragmentTab;
import gm.tieba.tabswitch.hooker.minus.FrsPageFilter;
import gm.tieba.tabswitch.hooker.minus.PersonalizedFilter;
import gm.tieba.tabswitch.hooker.minus.Purify;
import gm.tieba.tabswitch.hooker.minus.PurifyEnter;
import gm.tieba.tabswitch.hooker.minus.PurifyMy;
import gm.tieba.tabswitch.hooker.minus.RedTip;
import gm.tieba.tabswitch.hooker.minus.SwitchManager;

public abstract class BaseHooker {
    protected static ClassLoader sClassLoader;
    protected static Resources sRes;
    private static WeakReference<Context> sContextRef;

    protected BaseHooker() {
    }

    protected BaseHooker(ClassLoader classLoader, Context context, Resources res) {
        sClassLoader = classLoader;
        sContextRef = new WeakReference<>(context);
        sRes = res;
    }

    protected static Context getContext() {
        return sContextRef.get();
    }

    public static void init(Map.Entry<String, ?> entry) throws Throwable {
        switch (entry.getKey()) {
            case "home_recommend":
            case "fragment_tab":
                new FragmentTab().hook();
                break;
            case "switch_manager":
                new SwitchManager().hook();
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
                if ((Boolean) entry.getValue()) new FollowFilter().hook();
                break;
            case "personalized_filter":
                new PersonalizedFilter().hook();
                break;
            case "content_filter":
                new ContentFilter().hook();
                break;
            case "frs_page_filter":
                new FrsPageFilter().hook();
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
            case "ripple":
                if ((Boolean) entry.getValue()) new Ripple().hook();
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
                if ((Boolean) entry.getValue()) new OpenSign().hook();
                break;
            case "eyeshield_mode":
                if ((Boolean) entry.getValue()) new EyeshieldMode().hook();
                break;
            case "origin_src":
                if ((Boolean) entry.getValue()) new OriginSrc().hook();
                break;
            case "redirect_image":
                if ((Boolean) entry.getValue()) new RedirectImage().hook();
                break;
            case "forbid_gesture":
                if ((Boolean) entry.getValue()) new ForbidGesture().hook();
                break;
            case "agree_num":
                if (!(Boolean) entry.getValue()) break;
                XposedHelpers.findAndHookMethod("tbclient.Agree$Builder", sClassLoader,
                        "build", boolean.class, new XC_MethodHook() {
                            @Override
                            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                XposedHelpers.setObjectField(param.thisObject, "agree_num",
                                        XposedHelpers.getObjectField(param.thisObject, "diff_agree_num"));
                            }
                        });
                break;
            case "frs_tab":
                if ((Boolean) entry.getValue()) new FrsTab().hook();
                break;
            case "hide":
                if ((Boolean) entry.getValue()) new Hide().hook();
                break;
            case "check_stack_trace":
                if ((Boolean) entry.getValue()) new StackTrace().hook();
                break;
            case "check_xposed":
            case "check_module":
                // prevent from being removed
                break;
            default:
                if (!BuildConfig.DEBUG) Preferences.remove(entry.getKey());
                break;
        }
    }
}
