package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.widget.NavigationBar;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbEditText;
import gm.tieba.tabswitch.widget.TbToast;

public class HistoryCache extends XposedContext implements IHooker {
    private String mRegex = "";

    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.myCollection.history.PbHistoryActivity", sClassLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        new NavigationBar(param.thisObject)
                                .addTextButton("搜索", v -> showRegexDialog(activity));
                    }
                });
        for (Method method : XposedHelpers.findClass("com.baidu.tieba.myCollection.history.PbHistoryActivity", sClassLoader).getDeclaredMethods()) {
            if (Arrays.toString(method.getParameterTypes()).equals("[interface java.util.List]")) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        List<?> list = (List<?>) param.args[0];
                        if (list == null) return;
                        final Pattern pattern = Pattern.compile(mRegex);
                        list.removeIf((Predicate<Object>) o -> {
                            String[] strings;
                            try {
                                // com.baidu.tieba.myCollection.baseHistory.a
                                strings = new String[]{(String) XposedHelpers.getObjectField(o, "forumName"),
                                        (String) XposedHelpers.getObjectField(o, "threadName")};
                            } catch (NoSuchFieldError e) {
                                strings = new String[]{(String) XposedHelpers.getObjectField(o, "g"),
                                        (String) XposedHelpers.getObjectField(o, "f")};
                            }
                            for (String string : strings) {
                                if (pattern.matcher(string).find()) {
                                    return false;
                                }
                            }
                            return true;
                        });
                    }
                });
            }
        }
    }

    private void showRegexDialog(Activity activity) {
        EditText editText = new TbEditText(activity);
        editText.setHint(sRes.getString(R.string.regex_hint));
        editText.setText(mRegex);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mRegex = s.toString();
            }
        });
        TbDialog bdAlert = new TbDialog(activity, null, null, true, editText);
        bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
        bdAlert.setOnYesButtonClickListener(v -> {
            try {
                Pattern.compile(editText.getText().toString());
                bdAlert.dismiss();
                activity.recreate();
            } catch (PatternSyntaxException e) {
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
