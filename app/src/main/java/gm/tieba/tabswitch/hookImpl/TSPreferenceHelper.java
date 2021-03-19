package gm.tieba.tabswitch.hookImpl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.util.DisplayHelper;

public class TSPreferenceHelper extends Hook {
    static class PreferenceLayout {
        public List<SwitchViewHolder> switches;
        private LinearLayout linearLayout;

        PreferenceLayout(Activity activity) {
            linearLayout = new LinearLayout(activity);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setPadding(30, 0, 30, 0);
            switches = new ArrayList<>();
        }

        void addView(Object view) {
            if (view instanceof View) linearLayout.addView((View) view);
            else {
                linearLayout.addView(((SwitchViewHolder) view).newSwitch);
                switches.add((SwitchViewHolder) view);
            }
        }

        ScrollView create() {
            ScrollView scrollView = new ScrollView(linearLayout.getContext());
            scrollView.addView(linearLayout);
            return scrollView;
        }
    }

    static TextView generateTextView(Activity activity, String text) {
        TextView textView = new TextView(activity);
        textView.setText(text);
        textView.setTextColor(Hook.modRes.getColor(R.color.colorInstall, null));
        textView.setTextSize(17);
        textView.setPadding(20, 40, 0, 15);
        return textView;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    static LinearLayout generateButton(ClassLoader classLoader, Activity activity, String text, String tip, View.OnClickListener onClickListener) {
        try {
            Class<?> TbSettingTextTipView = classLoader.loadClass("com.baidu.tbadk.coreExtra.view.TbSettingTextTipView");
            Object instance = TbSettingTextTipView.getConstructor(Context.class).newInstance(activity);
            TbSettingTextTipView.getDeclaredMethod("setText", String.class).invoke(instance, text);
            TbSettingTextTipView.getDeclaredMethod("setTip", String.class).invoke(instance, tip);
            Field[] linearLayouts = instance.getClass().getDeclaredFields();
            for (Field linearLayout : linearLayouts) {
                linearLayout.setAccessible(true);
                if (linearLayout.get(instance) instanceof LinearLayout) {
                    LinearLayout newButton = (LinearLayout) linearLayout.get(instance);
                    ((ViewGroup) Objects.requireNonNull(newButton).getParent()).removeView(newButton);
                    if (!DisplayHelper.isLightMode(activity)) {
                        ((TextView) newButton.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("text").getInt(null))).setTextColor(Hook.modRes.getColor(R.color.colorPrimary, null));
                        ((TextView) newButton.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("tip").getInt(null))).setTextColor(Hook.modRes.getColor(R.color.colorPrimary, null));
                    }
                    newButton.setBackground(Hook.modRes.getDrawable(R.drawable.item_background_button, null));
                    if (onClickListener != null) newButton.setOnClickListener(onClickListener);
                    return newButton;
                }
            }
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
        throw new NullPointerException("generate button failed");
    }

    static class SwitchViewHolder {
        public LinearLayout newSwitch;
        public String text;
        public String key;
        public View bdSwitch;
        public ClassLoader classLoader;
        private Class<?> BdSwitchView;

        SwitchViewHolder(ClassLoader classLoader, Activity activity, String text, String key) {
            this.text = text;
            this.key = key;
            this.classLoader = classLoader;
            try {
                BdSwitchView = classLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView");
                bdSwitch = (View) BdSwitchView.getConstructor(Context.class).newInstance(activity);
                bdSwitch.setLayoutParams(new LinearLayout.LayoutParams(bdSwitch.getWidth(), bdSwitch.getHeight(), 0.25f));
                LinearLayout newSwitch = generateButton(classLoader, activity, text, null, v -> {
                    try {
                        Method changeState;
                        try {
                            changeState = BdSwitchView.getDeclaredMethod("changeState");
                        } catch (NoSuchMethodException e) {
                            changeState = BdSwitchView.getDeclaredMethod("b");
                        }
                        changeState.setAccessible(true);
                        changeState.invoke(bdSwitch);
                    } catch (Throwable throwable) {
                        XposedBridge.log(throwable);
                    }
                });
                newSwitch.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("arrow2").getInt(null)).setVisibility(View.GONE);
                newSwitch.addView(bdSwitch);
                SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
                if (!text.startsWith("过滤") && tsPreference.getBoolean(key, false) ||
                        text.startsWith("过滤") && tsPreference.getString(key, null) != null)
                    turnOn();
                else turnOff();
                this.newSwitch = newSwitch;
                XposedHelpers.findAndHookMethod("com.baidu.adp.widget.BdSwitchView.BdSwitchView", classLoader, "onDraw", Canvas.class, new XC_MethodHook() {
                    @SuppressLint("ApplySharedPref")
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        boolean isChangeingSate;
                        try {
                            isChangeingSate = (boolean) XposedHelpers.getObjectField(param.thisObject, "mIsChangeingSate");
                        } catch (NoSuchFieldError e) {
                            isChangeingSate = (boolean) XposedHelpers.getObjectField(param.thisObject, "m");
                        }
                        if (isChangeingSate || text.startsWith("过滤"))
                            return;
                        SharedPreferences.Editor editor = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE).edit();
                        editor.putBoolean(key, isOn());
                        editor.commit();
                    }
                });
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
        }

        boolean isOn() {
            try {
                try {
                    return (Boolean) BdSwitchView.getDeclaredMethod("isOn").invoke(bdSwitch);
                } catch (NoSuchMethodException e) {
                    return (Boolean) BdSwitchView.getDeclaredMethod("d").invoke(bdSwitch);
                }
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
            return false;
        }

        void turnOn() {
            try {
                try {
                    BdSwitchView.getDeclaredMethod("turnOn").invoke(bdSwitch);
                } catch (NoSuchMethodException e) {
                    BdSwitchView.getDeclaredMethod("i").invoke(bdSwitch);
                }
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
        }

        void turnOff() {
            try {
                try {
                    BdSwitchView.getDeclaredMethod("turnOff").invoke(bdSwitch);
                } catch (NoSuchMethodException e) {
                    BdSwitchView.getDeclaredMethod("f").invoke(bdSwitch);
                }
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
        switch (new Random().nextInt(5)) {
            case 0:
                return "别点了，新版本在做了";
            case 1:
                return "别点了别点了T_T";
            case 2:
                return "再点人傻了>_<";
            case 3:
                return "点了也没用~";
            case 4:
                return "点个小星星吧:)";
            default:
                return null;
        }
    }

    static class TbEditTextBuilder {
        public EditText editText;

        TbEditTextBuilder(ClassLoader classLoader, Context context) {
            editText = new EditText(context);
            editText.setFocusable(true);
            editText.setFocusableInTouchMode(true);
            editText.setTextSize(17);
            editText.requestFocus();
            editText.setHintTextColor(Hook.modRes.getColor(R.color.colorProgress, null));
            if (!DisplayHelper.isLightMode(context))
                editText.setTextColor(modRes.getColor(R.color.colorPrimary, null));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            editText.setLayoutParams(layoutParams);
            try {
                editText.setBackgroundResource(classLoader.loadClass("com.baidu.tieba.R$drawable").getField("blue_rectangle_input_bg").getInt(null));
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
        }
    }

    public static class TbDialogBuilder {
        public ViewGroup mRootView;
        private Object bdalert;
        private Class<?> TbDialog;
        private final ClassLoader classLoader;
        private Object pageContext;
        private AlertDialog mDialog;

        public TbDialogBuilder(ClassLoader classLoader, Context context, String title, String message, boolean cancelable, View contentView) {
            XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.BaseFragment", classLoader, "getPageContext", new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    pageContext = param.getResult();
                }
            });
            this.classLoader = classLoader;
            for (int i = 0; i < ruleMapList.size(); i++) {
                Map<String, String> map = ruleMapList.get(i);
                if (Objects.equals(map.get("rule"), "Lcom/baidu/tieba/R$layout;->dialog_bdalert:I")) {
                    try {
                        TbDialog = classLoader.loadClass(map.get("class"));
                        bdalert = TbDialog.getConstructor(Activity.class).newInstance((Activity) context);
                        try {
                            mRootView = (ViewGroup) XposedHelpers.getObjectField(bdalert, "mRootView");
                            XposedHelpers.setObjectField(bdalert, "mTitle", title);
                            XposedHelpers.setObjectField(bdalert, "mMessage", message);
                            XposedHelpers.setObjectField(bdalert, "mContentView", contentView);
                            XposedHelpers.setObjectField(bdalert, "mPositiveButtonTip", "确定");
                            XposedHelpers.setObjectField(bdalert, "mNegativeButtonTip", "取消");
                            if (!cancelable)
                                XposedHelpers.setObjectField(bdalert, "mCancelable", false);
                        } catch (NoSuchFieldError e) {
                            mRootView = (ViewGroup) XposedHelpers.getObjectField(bdalert, "y");
                            XposedHelpers.setObjectField(bdalert, "f", title);
                            XposedHelpers.setObjectField(bdalert, "h", message);
                            XposedHelpers.setObjectField(bdalert, "g", contentView);
                            XposedHelpers.setObjectField(bdalert, "l", "确定");
                            XposedHelpers.setObjectField(bdalert, "m", "取消");
                            if (!cancelable) XposedHelpers.setObjectField(bdalert, "C", false);
                        }
                    } catch (Throwable throwable) {
                        XposedBridge.log(throwable);
                    }
                    return;
                }
            }
            throw new NullPointerException("create tb dialog failed");
        }

        public void setOnYesButtonClickListener(View.OnClickListener onClickListener) {
            try {
                mRootView.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("yes").getInt(null)).setOnClickListener(onClickListener);
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
        }

        public void setOnNoButtonClickListener(View.OnClickListener onClickListener) {
            try {
                mRootView.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("no").getInt(null)).setOnClickListener(onClickListener);
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
        }

        public void show() {
            try {
                Method[] methods = TbDialog.getDeclaredMethods();
                for (Method method : methods)
                    if (Arrays.toString(method.getParameterTypes()).startsWith("[interface") && !Arrays.toString(method.getParameterTypes()).contains("$"))
                        method.invoke(bdalert, pageContext);// create
                for (Method method : methods)
                    if (Arrays.toString(method.getParameterTypes()).equals("[]") && Objects.equals(method.getReturnType(), TbDialog)) {
                        method.invoke(bdalert);// show
                        break;
                    }
                try {
                    mDialog = (AlertDialog) XposedHelpers.getObjectField(bdalert, "mDialog");
                } catch (NoSuchFieldError e) {
                    mDialog = (AlertDialog) XposedHelpers.getObjectField(bdalert, "w");
                }
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
        }

        public Window getWindow() {
            return mDialog.getWindow();
        }

        public void dismiss() {
            try {
                try {
                    TbDialog.getDeclaredMethod("dismiss").invoke(bdalert);
                } catch (NoSuchMethodException e) {
                    TbDialog.getDeclaredMethod("k").invoke(bdalert);
                }
            } catch (Throwable throwable) {
                XposedBridge.log(throwable);
            }
        }
    }
}