package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

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
import gm.tieba.tabswitch.util.ReflectUtils;
import gm.tieba.tabswitch.widget.NavigationBar;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbEditText;
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

                final var pattern = Pattern.compile(mRegex);
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
        final EditText editText = new TbEditText(activity);
        editText.setHint(Constants.getStrings().get("regex_hint"));
        editText.setText(mRegex);
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
        final TbDialog bdAlert = new TbDialog(activity, null, null, true, editText);
        bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
        bdAlert.setOnYesButtonClickListener(v -> {
            try {
                Pattern.compile(editText.getText().toString());
                bdAlert.dismiss();
                activity.recreate();
            } catch (final PatternSyntaxException e) {
                TbToast.showTbToast(e.getMessage(), TbToast.LENGTH_SHORT);
            }
        });
        bdAlert.show();
        bdAlert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        editText.setSingleLine();
        editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                bdAlert.findYesButton().performClick();
                return true;
            }
            return false;
        });
        editText.selectAll();
        editText.requestFocus();
    }
}
