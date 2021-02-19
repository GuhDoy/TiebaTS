package gm.tieba.tabswitch.hookImpl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.R;

class TSPreferenceHelper extends Hook {
    static TextView generateTextView(Activity activity, String text) {
        try {
            TextView textView = new TextView(activity);
            textView.setText(text);
            textView.setTextColor(Hook.modRes.getColor(R.color.colorInstall, null));
            textView.setTextSize(17);
            textView.setPadding(20, 40, 0, 15);
            return textView;
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
        return null;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    static LinearLayout generateButton(XC_LoadPackage.LoadPackageParam lpparam, Activity activity, String text, String tip) {
        try {
            Class<?> TbSettingTextTipView = lpparam.classLoader.loadClass("com.baidu.tbadk.coreExtra.view.TbSettingTextTipView");
            Object instance = TbSettingTextTipView.getConstructor(Context.class).newInstance(activity);
            TbSettingTextTipView.getDeclaredMethod("setText", String.class).invoke(instance, text);
            if (tip != null)
                TbSettingTextTipView.getDeclaredMethod("setTip", String.class).invoke(instance, tip);
            Field[] linearLayouts = instance.getClass().getDeclaredFields();
            for (Field linearLayout : linearLayouts) {
                linearLayout.setAccessible(true);
                if (linearLayout.get(instance) instanceof LinearLayout) {
                    LinearLayout newButton = (LinearLayout) linearLayout.get(instance);
                    ((ViewGroup) Objects.requireNonNull(newButton).getParent()).removeView(newButton);
                    if ((activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                        ((TextView) newButton.findViewById(lpparam.classLoader.loadClass("com.baidu.tieba.R$id").getField("text").getInt(null))).setTextColor(Hook.modRes.getColor(R.color.colorPrimary, null));
                        ((TextView) newButton.findViewById(lpparam.classLoader.loadClass("com.baidu.tieba.R$id").getField("tip").getInt(null))).setTextColor(Hook.modRes.getColor(R.color.colorPrimary, null));
                    }
                    newButton.setBackground(Hook.modRes.getDrawable(R.drawable.item_background_button, null));
                    return newButton;
                }
            }
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
        return null;
    }

    static class SwitchViewHolder {
        public LinearLayout newSwitch;
        public View switchInstance;
        private ClassLoader classLoader;

        SwitchViewHolder(XC_LoadPackage.LoadPackageParam lpparam, Activity activity, String text, boolean isTurnOn) {
            this.classLoader = lpparam.classLoader;
            try {
                LinearLayout newSwitch = generateButton(lpparam, activity, text, null);
                newSwitch.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("arrow2").getInt(null)).setVisibility(View.GONE);
                Class<?> BdSwitchView = classLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView");
                switchInstance = (View) BdSwitchView.getConstructor(Context.class).newInstance(activity);
                switchInstance.setLayoutParams(new LinearLayout.LayoutParams(switchInstance.getWidth(), switchInstance.getHeight(), 0.25f));
                newSwitch.addView(switchInstance);
                if (isTurnOn) turnOn();
                else turnOff();
                newSwitch.setOnClickListener(v -> {
                    try {
                        Method changeState = BdSwitchView.getDeclaredMethod("changeState");
                        changeState.setAccessible(true);
                        changeState.invoke(switchInstance);
                    } catch (Throwable throwable) {
                        XposedBridge.log(throwable);
                    }
                });
                this.newSwitch = newSwitch;
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
        }

        boolean isOn() {
            try {
                Class<?> BdSwitchView = classLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView");
                return (Boolean) BdSwitchView.getDeclaredMethod("isOn").invoke(switchInstance);
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
            return false;
        }

        void turnOn() {
            try {
                Class<?> BdSwitchView = classLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView");
                BdSwitchView.getDeclaredMethod("turnOn").invoke(switchInstance);
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
        }

        void turnOff() {
            try {
                Class<?> BdSwitchView = classLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView");
                BdSwitchView.getDeclaredMethod("turnOff").invoke(switchInstance);
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
        }
    }

    static Intent launchModuleIntent(Activity activity) {
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory("de.robv.android.xposed.category.MODULE_SETTINGS");
        intentToResolve.setPackage("gm.tieba.tabswitch");
        List<ResolveInfo> ris = activity.getPackageManager().queryIntentActivities(intentToResolve, 0);
        if (ris.size() > 0) return intentToResolve;
        else return null;
    }

    static String randomToast() {
        switch (new Random().nextInt(4)) {
            case 0:
                return "别点了，新版本在做了";
            case 1:
                return "别点了别点了T_T";
            case 2:
                return "再点人傻了>_<";
            case 3:
                return "点了也没用~";
            default:
                return null;
        }
    }
}