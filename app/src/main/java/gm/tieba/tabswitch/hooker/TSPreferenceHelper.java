package gm.tieba.tabswitch.hooker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbEditText;
import gm.tieba.tabswitch.widget.TbToast;
import gm.tieba.tabswitch.util.DisplayHelper;

public class TSPreferenceHelper {
    static class PreferenceLayout extends LinearLayout {
        PreferenceLayout(Context context) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
        }

        void addView(Object view) {
            if (view instanceof View) addView((View) view);
            else addView(((SwitchHolder) view).newSwitch);
        }
    }

    static TextView createTextView(ClassLoader classLoader, Activity activity, String text) {
        try {
            TextView textView = new TextView(activity);
            textView.setText(text);
            textView.setTextColor(activity.getColor(classLoader.loadClass("com.baidu.tieba.R$color").getField("CAM_X0108").getInt(null)));
            textView.setTextSize(DisplayHelper.px2Dip(activity, activity.getResources().getDimension(classLoader.loadClass("com.baidu.tieba.R$dimen").getField("fontsize28").getInt(null))));
            LinearLayout.LayoutParams layoutParams;
            if (text != null) {
                textView.setPadding((int) activity.getResources().getDimension(classLoader.loadClass("com.baidu.tieba.R$dimen").getField("ds30").getInt(null)),
                        (int) activity.getResources().getDimension(classLoader.loadClass("com.baidu.tieba.R$dimen").getField("ds32").getInt(null)), 0,
                        (int) activity.getResources().getDimension(classLoader.loadClass("com.baidu.tieba.R$dimen").getField("ds10").getInt(null)));
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            } else {
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) activity.getResources().getDimension(classLoader.loadClass("com.baidu.tieba.R$dimen").getField("ds32").getInt(null)));
            }
            textView.setLayoutParams(layoutParams);
            return textView;
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
        throw new NullPointerException("create text view failed");
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    static LinearLayout createButton(ClassLoader classLoader, Activity activity, String text, String tip, View.OnClickListener onClick) {
        try {
            Class<?> TbSettingTextTipView = classLoader.loadClass("com.baidu.tbadk.coreExtra.view.TbSettingTextTipView");
            Object instance = TbSettingTextTipView.getConstructor(Context.class).newInstance(activity);
            TbSettingTextTipView.getDeclaredMethod("setText", String.class).invoke(instance, text);
            TbSettingTextTipView.getDeclaredMethod("setTip", String.class).invoke(instance, tip);
            for (Field field : instance.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.get(instance) instanceof LinearLayout) {
                    LinearLayout newButton = (LinearLayout) field.get(instance);
                    ((ViewGroup) Objects.requireNonNull(newButton).getParent()).removeView(newButton);
                    if (onClick != null) newButton.setOnClickListener(onClick);
                    return newButton;
                }
            }
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
        throw new NullPointerException("create button failed");
    }

    //TODO: 封装SwitchHolder，此类改名为createSwitchButton
    static class SwitchHolder {
        public final static int TYPE_SWITCH = 0;
        public final static int TYPE_DIALOG = 1;
        public final static int TYPE_SET = 2;
        private final ClassLoader mClassLoader;
        private Class<?> mClass;
        private final String mKey;
        public LinearLayout newSwitch;
        public View bdSwitch;

        @SuppressLint("ClickableViewAccessibility")
        SwitchHolder(ClassLoader classLoader, Activity activity, Resources res, String text, String key, int type) {
            mClassLoader = classLoader;
            mKey = key;
            try {
                mClass = classLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView");
                bdSwitch = (View) mClass.getConstructor(Context.class).newInstance(activity);
                bdSwitch.setLayoutParams(new LinearLayout.LayoutParams(bdSwitch.getWidth(), bdSwitch.getHeight(), 0.25f));
                setOnSwitchStateChangeListener();
                switch (type) {
                    case TYPE_SWITCH:
                        newSwitch = createButton(classLoader, activity, text, null, v -> changeState());
                        bdSwitch.setTag("SWITCH" + key);
                        if (Preferences.getBoolean(key)) turnOn();
                        else turnOff();
                        break;
                    case TYPE_DIALOG:
                        newSwitch = createButton(classLoader, activity, text, null, v -> showRegexDialog(activity, res));
                        bdSwitch.setOnTouchListener((v, event) -> false);
                        if (Preferences.getString(key) != null) turnOn();
                        else turnOff();
                        break;
                    case TYPE_SET:
                        newSwitch = createButton(classLoader, activity, text, null, v -> changeState());
                        bdSwitch.setTag("SET" + key);
                        if (Preferences.getStringSet().contains(key)) turnOn();
                        else turnOff();
                        break;
                }
                newSwitch.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("arrow2").getInt(null)).setVisibility(View.GONE);
                newSwitch.addView(bdSwitch);
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }

        private void setOnSwitchStateChangeListener() {
            try {
                Class<?> OnSwitchStateChangeListener;
                try {
                    OnSwitchStateChangeListener = mClassLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView$b");
                } catch (ClassNotFoundException e) {
                    OnSwitchStateChangeListener = mClassLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView$a");
                }
                Object proxy = Proxy.newProxyInstance(mClassLoader, new Class<?>[]{OnSwitchStateChangeListener}, new MyInvocationHandler());
                mClass.getDeclaredMethod("setOnSwitchStateChangeListener",
                        OnSwitchStateChangeListener).invoke(bdSwitch, proxy);
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }

        private static class MyInvocationHandler implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                View view = (View) args[0];
                String key = (String) view.getTag();
                if (key != null) {
                    if (key.startsWith("SWITCH")) {
                        Preferences.putBoolean(key.replace("SWITCH", ""), args[1].toString().equals("ON"));
                    } else if (key.startsWith("SET")) {
                        Preferences.putStringSet(key.replace("SET", ""), args[1].toString().equals("ON"));
                    }
                }
                return null;
            }
        }

        private final Map<String, String> mRegex = new HashMap<>();

        private void showRegexDialog(Activity activity, Resources res) {
            EditText editText = new TbEditText(mClassLoader, activity, res);
            editText.setHint("请输入正则表达式，如.*");
            String text = mRegex.get(mKey);
            if (text == null) text = Preferences.getString(mKey);
            editText.setText(text);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    mRegex.put(mKey, s.toString());
                }
            });
            TbDialog bdAlert = new TbDialog(mClassLoader, activity, null, null, true, editText);
            bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
            bdAlert.setOnYesButtonClickListener(v -> {
                try {
                    if (TextUtils.isEmpty(editText.getText())) {
                        Preferences.putString(mKey, null);
                        turnOff();
                    } else {
                        Pattern.compile(editText.getText().toString());
                        Preferences.putString(mKey, editText.getText().toString());
                        turnOn();
                    }
                    bdAlert.dismiss();
                } catch (PatternSyntaxException e) {
                    TbToast.showTbToast(mClassLoader, activity, e.getMessage(), TbToast.LENGTH_SHORT);
                }
            });
            bdAlert.show();
            bdAlert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    bdAlert.getYesButton().performClick();
                    return true;
                }
                return false;
            });
            editText.requestFocus();
        }

        boolean isOn() {
            try {
                try {
                    return (Boolean) mClass.getDeclaredMethod("isOn").invoke(bdSwitch);
                } catch (NoSuchMethodException e) {
                    return (Boolean) mClass.getDeclaredMethod("d").invoke(bdSwitch);
                }
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
            return false;
        }

        void changeState() {
            try {
                Method changeState;
                try {
                    changeState = mClass.getDeclaredMethod("changeState");
                } catch (NoSuchMethodException e) {
                    changeState = mClass.getDeclaredMethod("b");
                }
                changeState.setAccessible(true);
                changeState.invoke(bdSwitch);
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }

        void turnOn() {
            try {
                try {
                    mClass.getDeclaredMethod("turnOn").invoke(bdSwitch);
                } catch (NoSuchMethodException e) {
                    mClass.getDeclaredMethod("i").invoke(bdSwitch);
                }
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }

        void turnOff() {
            try {
                try {
                    mClass.getDeclaredMethod("turnOff").invoke(bdSwitch);
                } catch (NoSuchMethodException e) {
                    mClass.getDeclaredMethod("f").invoke(bdSwitch);
                }
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }
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
                return "搞个大新闻";
        }
    }
}
