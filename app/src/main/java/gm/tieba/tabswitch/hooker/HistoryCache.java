package gm.tieba.tabswitch.hooker;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.Hooker;
import gm.tieba.tabswitch.hooker.model.TbDialogBuilder;
import gm.tieba.tabswitch.hooker.model.TbEditText;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.Reflect;

public class HistoryCache extends BaseHooker implements Hooker {
    private String mRegex = "";

    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.myCollection.history.PbHistoryActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Object navigationBar = Reflect.getObjectField(param.thisObject, "com.baidu.tbadk.core.view.NavigationBar");
                Class<?> ControlAlign = sClassLoader.loadClass("com.baidu.tbadk.core.view.NavigationBar$ControlAlign");
                for (Object HORIZONTAL_RIGHT : ControlAlign.getEnumConstants()) {
                    if (HORIZONTAL_RIGHT.toString().equals("HORIZONTAL_RIGHT")) {
                        Class<?> NavigationBar = sClassLoader.loadClass("com.baidu.tbadk.core.view.NavigationBar");
                        TextView textView = (TextView) NavigationBar.getDeclaredMethod("addTextButton", ControlAlign, String.class, View.OnClickListener.class)
                                .invoke(navigationBar, HORIZONTAL_RIGHT, "搜索", (View.OnClickListener) v -> showRegexDialog(activity));
                        if (DisplayHelper.isLightMode(activity)) {
                            textView.setTextColor(Color.parseColor("#FF3E3D40"));
                        } else {
                            textView.setTextColor(Color.parseColor("#FFCBCBCC"));
                        }
                        return;
                    }
                }
            }
        });
        for (Method method : sClassLoader.loadClass("com.baidu.tieba.myCollection.history.PbHistoryActivity").getDeclaredMethods()) {
            if (Arrays.toString(method.getParameterTypes()).equals("[interface java.util.List]")) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        List<?> list = (List<?>) param.args[0];
                        if (list == null) return;
                        final Pattern pattern = Pattern.compile(mRegex);
                        label:
                        for (int j = 0; j < list.size(); j++) {
                            String[] strings;
                            try {
                                // com.baidu.tieba.myCollection.baseHistory.a
                                strings = new String[]{(String) XposedHelpers.getObjectField(list.get(j), "forumName"),
                                        (String) XposedHelpers.getObjectField(list.get(j), "threadName")};
                            } catch (NoSuchFieldError e) {
                                strings = new String[]{(String) XposedHelpers.getObjectField(list.get(j), "g"),
                                        (String) XposedHelpers.getObjectField(list.get(j), "f")};
                            }
                            for (String string : strings) {
                                if (pattern.matcher(string).find()) {
                                    continue label;
                                }
                            }
                            list.remove(j);
                            j--;
                        }
                    }
                });
            }
        }
    }

    private void showRegexDialog(Activity activity) {
        EditText editText = new TbEditText(sClassLoader, activity, sRes);
        editText.setHint("请输入正则表达式，如.*");
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
        TbDialogBuilder bdAlert = new TbDialogBuilder(sClassLoader, activity, null, null, true, editText);
        bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
        bdAlert.setOnYesButtonClickListener(v -> {
            try {
                Pattern.compile(editText.getText().toString());
                activity.finish();
                activity.startActivity(activity.getIntent());
                bdAlert.dismiss();
            } catch (PatternSyntaxException e) {
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        bdAlert.show();
        bdAlert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        editText.setSingleLine();
        editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                bdAlert.getYesButton().performClick();
                return true;
            }
            return false;
        });
        editText.selectAll();
        editText.requestFocus();
    }
}