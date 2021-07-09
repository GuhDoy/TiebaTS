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
import gm.tieba.tabswitch.hooker.auto.FrsTab;
import gm.tieba.tabswitch.hooker.auto.OpenSign;
import gm.tieba.tabswitch.hooker.auto.OriginSrc;
import gm.tieba.tabswitch.hooker.extra.ForbidGesture;
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

public abstract class XposedWrapper {
    protected static WeakReference<Context> sContextRef;
    protected static ClassLoader sClassLoader;
    protected static Resources sRes;
    public static String sPath;

    protected XposedWrapper() {
    }

    protected static Context getContext() {
        return sContextRef.get();
    }
}
