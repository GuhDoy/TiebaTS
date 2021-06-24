package gm.tieba.tabswitch.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.widget.TbToast;

public class DisplayUtils {
    public static boolean isLightMode(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_NO;
    }

    public static void restart(Activity activity, Resources res) {
        Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity
                .getPackageName());
        if (intent == null) {
            String hint = "获取启动意图失败，请手动启动应用";
            if (AcRules.isRuleFound(res.getString(R.string.TbToast))) {
                TbToast.showTbToast(hint, TbToast.LENGTH_SHORT);
            } else {
                Toast.makeText(activity, hint, Toast.LENGTH_SHORT).show();
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> System.exit(0), 1000);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            System.exit(0);
        }
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

    public static int dipToPx(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int pxToDip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
