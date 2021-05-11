package gm.tieba.tabswitch.hooker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.widget.Switch;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbEditText;
import gm.tieba.tabswitch.widget.TbToast;

public class TSPreferenceHelper {
    static class PreferenceLayout extends LinearLayout {
        PreferenceLayout(Context context) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
        }

        void addView(Object view) {
            if (view instanceof View) addView((View) view);
            else addView(((SwitchButtonHolder) view).switchButton);
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

    @SuppressLint("ClickableViewAccessibility")
    static class SwitchButtonHolder {
        public final static int TYPE_SWITCH = 0;
        public final static int TYPE_DIALOG = 1;
        public final static int TYPE_SET = 2;
        private final ClassLoader mClassLoader;
        private final String mKey;
        private final Resources mRes;
        public Switch bdSwitch;
        public LinearLayout switchButton;

        SwitchButtonHolder(ClassLoader classLoader, Activity activity, Resources res, String text,
                           String key, int type) {
            mClassLoader = classLoader;
            mRes = res;
            mKey = key;
            bdSwitch = new Switch(classLoader, activity);
            bdSwitch.setOnSwitchStateChangeListener(new SwitchStatusChangeHandler());
            View bdSwitchView = bdSwitch.bdSwitch;
            bdSwitchView.setLayoutParams(new LinearLayout.LayoutParams(bdSwitchView.getWidth(),
                    bdSwitchView.getHeight(), 0.25f));
            switch (type) {
                case TYPE_SWITCH:
                    switchButton = createButton(classLoader, activity, text, null, v -> bdSwitch.changeState());
                    bdSwitchView.setTag(TYPE_SWITCH + key);
                    if (Preferences.getBoolean(key)) bdSwitch.turnOn();
                    else bdSwitch.turnOff();
                    break;
                case TYPE_DIALOG:
                    switchButton = createButton(classLoader, activity, text, null, v -> showRegexDialog(activity, res));
                    bdSwitchView.setOnTouchListener((v, event) -> false);
                    if (Preferences.getString(key) != null) bdSwitch.turnOn();
                    else bdSwitch.turnOff();
                    break;
                case TYPE_SET:
                    switchButton = createButton(classLoader, activity, text, null, v -> bdSwitch.changeState());
                    bdSwitchView.setTag(TYPE_SET + key);
                    if (Preferences.getStringSet().contains(key)) bdSwitch.turnOn();
                    else bdSwitch.turnOff();
                    break;
            }
            try {
                switchButton.findViewById(classLoader.loadClass("com.baidu.tieba.R$id")
                        .getField("arrow2").getInt(null)).setVisibility(View.GONE);
                switchButton.addView(bdSwitchView);
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }

        void setOnButtonClickListener(View.OnClickListener onClick) {
            switchButton.setOnClickListener(onClick);
            bdSwitch.bdSwitch.setOnTouchListener((View v, MotionEvent event) -> false);
        }

        private static class SwitchStatusChangeHandler implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                View view = (View) args[0];
                String key = (String) view.getTag();
                if (key != null) {
                    if (key.startsWith(String.valueOf(TYPE_SWITCH))) {
                        Preferences.putBoolean(key.substring(1), args[1].toString().equals("ON"));
                    } else if (key.startsWith(String.valueOf(TYPE_SET))) {
                        Preferences.putStringSet(key.substring(1), args[1].toString().equals("ON"));
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
            TbDialog bdAlert = new TbDialog(mClassLoader, activity, mRes, null, null, true, editText);
            bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
            bdAlert.setOnYesButtonClickListener(v -> {
                try {
                    if (TextUtils.isEmpty(editText.getText())) {
                        Preferences.putString(mKey, null);
                        bdSwitch.turnOff();
                    } else {
                        Pattern.compile(editText.getText().toString());
                        Preferences.putString(mKey, editText.getText().toString());
                        bdSwitch.turnOn();
                    }
                    bdAlert.dismiss();
                } catch (PatternSyntaxException e) {
                    TbToast.showTbToast(mClassLoader, activity, res, e.getMessage(), TbToast.LENGTH_SHORT);
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
