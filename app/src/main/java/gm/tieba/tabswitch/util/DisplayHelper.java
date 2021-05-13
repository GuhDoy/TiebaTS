package gm.tieba.tabswitch.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

public class DisplayHelper {
    public static boolean isLightMode(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_NO;
    }

    public static String getTbSkin(Context context) {
        SharedPreferences sp = context.getSharedPreferences("common_settings",
                Context.MODE_PRIVATE);
        switch (sp.getString("skin_", "0")) {
            case "4":
                return "_2";
            case "1":
                return "_1";
            case "0":
            default:
                return "";
        }
    }

    public static int dip2Px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int px2Dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
