package gm.tieba.tabswitch.hooker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
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
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.util.Reflect;
import gm.tieba.tabswitch.widget.Switch;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbEditText;
import gm.tieba.tabswitch.widget.TbToast;

public class TSPreferenceHelper extends BaseHooker {
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

    static TextView createTextView(String text) {
        try {
            TextView textView = new TextView(getContext());
            textView.setText(text);
            textView.setTextColor(Reflect.getColor("CAM_X0108"));
            textView.setTextSize(Reflect.getDimenDip("fontsize28"));
            LinearLayout.LayoutParams layoutParams;
            if (text != null) {
                textView.setPadding((int) Reflect.getDimen("ds30"),
                        (int) Reflect.getDimen("ds32"), 0,
                        (int) Reflect.getDimen("ds10"));
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            } else {
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) Reflect.getDimen("ds32"));
            }
            textView.setLayoutParams(layoutParams);
            return textView;
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
        throw new NullPointerException("create text view failed");
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    static LinearLayout createButton(String text, String tip, View.OnClickListener l) {
        try {
            Class<?> TbSettingTextTipView = sClassLoader.loadClass(
                    "com.baidu.tbadk.coreExtra.view.TbSettingTextTipView");
            Object instance = TbSettingTextTipView.getConstructor(Context.class).newInstance(getContext());
            TbSettingTextTipView.getDeclaredMethod("setText", String.class).invoke(instance, text);
            TbSettingTextTipView.getDeclaredMethod("setTip", String.class).invoke(instance, tip);
            for (Field field : instance.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.get(instance) instanceof LinearLayout) {
                    LinearLayout newButton = (LinearLayout) field.get(instance);
                    ((ViewGroup) Objects.requireNonNull(newButton).getParent()).removeView(newButton);
                    if (l != null) newButton.setOnClickListener(l);
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
        private final String mKey;
        public Switch bdSwitch;
        public LinearLayout switchButton;

        SwitchButtonHolder(Activity activity, String text, String key, int type) {
            mKey = key;
            bdSwitch = new Switch();
            bdSwitch.setOnSwitchStateChangeListener(new SwitchStatusChangeHandler());
            View bdSwitchView = bdSwitch.bdSwitch;
            bdSwitchView.setLayoutParams(new LinearLayout.LayoutParams(bdSwitchView.getWidth(),
                    bdSwitchView.getHeight(), 0.25f));
            switch (type) {
                case TYPE_SWITCH:
                    switchButton = createButton(text, null, v -> bdSwitch.changeState());
                    bdSwitchView.setTag(TYPE_SWITCH + key);
                    if (Preferences.getBoolean(key)) bdSwitch.turnOn();
                    else bdSwitch.turnOff();
                    break;
                case TYPE_DIALOG:
                    switchButton = createButton(text, null, v -> showRegexDialog(activity));
                    bdSwitchView.setOnTouchListener((v, event) -> false);
                    if (Preferences.getString(key) != null) bdSwitch.turnOn();
                    else bdSwitch.turnOff();
                    break;
                case TYPE_SET:
                    switchButton = createButton(text, null, v -> bdSwitch.changeState());
                    bdSwitchView.setTag(TYPE_SET + key);
                    if (Preferences.getStringSet().contains(key)) bdSwitch.turnOn();
                    else bdSwitch.turnOff();
                    break;
            }
            try {
                switchButton.findViewById(Reflect.getId("arrow2"))
                        .setVisibility(View.GONE);
                switchButton.addView(bdSwitchView);
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }

        void setOnButtonClickListener(View.OnClickListener l) {
            switchButton.setOnClickListener(l);
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

        private void showRegexDialog(Activity activity) {
            EditText editText = new TbEditText(getContext());
            editText.setHint("请输入正则表达式，如.*");
            editText.setText(Preferences.getString(mKey));
            TbDialog bdAlert = new TbDialog(activity, null, null, true, editText);
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
                    TbToast.showTbToast(e.getMessage(), TbToast.LENGTH_SHORT);
                }
            });
            bdAlert.show();
            bdAlert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
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
