package gm.tieba.tabswitch.hooker.auto;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Calendar;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.IHooker;

public class OpenSign extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "open_sign";
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                if (!Preferences.getIsSigned() && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) != 0) {
                    final Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.signall.SignAllForumActivity");
                    activity.startActivity(intent);
                }
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.signall.SignAllForumActivity", sClassLoader, "onClick", View.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                if (!Preferences.getIsSigned() && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) != 0) {
                    Preferences.putSignDate();
                    activity.finish();
                }
            }
        });
    }
}
