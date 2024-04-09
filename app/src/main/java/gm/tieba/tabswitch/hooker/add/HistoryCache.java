package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.util.ReflectUtils;
import gm.tieba.tabswitch.widget.NavigationBar;
import gm.tieba.tabswitch.widget.TbToast;

public class HistoryCache extends XposedContext implements IHooker {
    private String mRegex = "";

    @NonNull
    @Override
    public String key() {
        return "history_cache";
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.myCollection.history.PbHistoryActivity", sClassLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        final var activity = (Activity) param.thisObject;
                        new NavigationBar(param.thisObject)
                                .addTextButton("搜索", v -> showRegexDialog(activity));
                    }
                });
        final var method = ReflectUtils.findFirstMethodByExactType(
                "com.baidu.tieba.myCollection.history.PbHistoryActivity", List.class
        );
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final var list = (List<?>) param.args[0];
                if (list == null) return;

                final var pattern = Pattern.compile(mRegex, Pattern.CASE_INSENSITIVE);
                list.removeIf(o -> {
                    String[] strings;
                    try {
                        strings = new String[]{(String) XposedHelpers.getObjectField(o, "forumName"),
                                (String) XposedHelpers.getObjectField(o, "threadName")};
                    } catch (final NoSuchFieldError e) {
                        strings = new String[]{(String) ReflectUtils.getObjectField(o, 3),
                                (String) ReflectUtils.getObjectField(o, 2)};
                    }
                    for (final var string : strings) {
                        if (pattern.matcher(string).find()) {
                            return false;
                        }
                    }
                    return true;
                });
            }
        });
    }

    private void showRegexDialog(final Activity activity) {
        Activity currentActivity = ReflectUtils.getCurrentActivity();
        boolean isLightMode = DisplayUtils.isLightMode(getContext());

        final EditText editText = new EditText(currentActivity);
        editText.setHint(Constants.getStrings().get("regex_hint"));
        editText.setText(mRegex);
        if (!isLightMode) {
            editText.setTextColor(Color.WHITE);
            editText.setHintTextColor(Color.GRAY);
        }
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setFallbackLineSpacing(false);
        editText.setLineSpacing(0, 1.2F);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                mRegex = s.toString();
            }
        });

        final LinearLayout linearLayout = new LinearLayout(currentActivity);
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.leftMargin = DisplayUtils.dipToPx(currentActivity, 20F);
        layoutParams.rightMargin = DisplayUtils.dipToPx(currentActivity, 20F);
        editText.setLayoutParams(layoutParams);

        linearLayout.addView(editText);

        String currRegex = mRegex;

        AlertDialog alert = new AlertDialog.Builder(currentActivity, isLightMode ?
                android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("搜索").setView(linearLayout)
                .setOnCancelListener(dialog -> mRegex = currRegex)
                .setNegativeButton(activity.getString(android.R.string.cancel), (dialogInterface, i) -> mRegex = currRegex)
                .setPositiveButton(activity.getString(android.R.string.ok), null).create();

        alert.setOnShowListener(dialogInterface -> {
            Button button = alert.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                try {
                    Pattern.compile(editText.getText().toString());
                    alert.dismiss();
                    activity.recreate();
                } catch (final PatternSyntaxException e) {
                    TbToast.showTbToast(e.getMessage(), TbToast.LENGTH_SHORT);
                }
            });
        });

        alert.show();
        DisplayUtils.fixAlertDialogWidth(alert);

        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        editText.setSingleLine();
        editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                alert.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
        editText.selectAll();
        editText.requestFocus();
    }
}
