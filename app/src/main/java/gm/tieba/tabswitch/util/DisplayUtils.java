package gm.tieba.tabswitch.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;

public class DisplayUtils extends XposedContext {
    public static boolean isLightMode(final Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_NO;
    }

    public static void restart(final Activity activity) {
        final var intent = activity.getPackageManager().getLaunchIntentForPackage(activity
                .getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            System.exit(0);
        }
    }

    public static String getTbSkin(final Context context) {
        //Lcom/baidu/tbadk/core/TbadkCoreApplication;->getSkinType()I
        int skinType;
        try {
            Object instance = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.baidu.tbadk.core.TbadkCoreApplication", sClassLoader), "getInst");
            skinType = (int) XposedHelpers.callMethod(instance, "getSkinType");
        } catch (Exception e) {
            XposedBridge.log(e);
            final var settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
            if (settings.getBoolean("key_is_follow_system_mode", false)) {
                return isLightMode(context) ? "" : "_2";
            } else {
                final var commonSettings = context.getSharedPreferences(
                        "common_settings", Context.MODE_PRIVATE);
                skinType = Integer.parseInt((commonSettings.getString("skin_", "0")));
            }
        }
        switch (skinType) {
            case 1:
            case 4:
                return "_2";
            case 0:
            default:
                return "";
        }
    }

    public static int dipToPx(final Context context, final float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int pxToDip(final Context context, final float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
