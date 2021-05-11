package gm.tieba.tabswitch.hooker;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.Rule;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbEditText;
import gm.tieba.tabswitch.widget.TbToast;

public class ThreadStore extends BaseHooker implements IHooker {
    private String mRegex = "";

    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.myCollection.CollectTabActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                TextView edit = activity.findViewById(sClassLoader.loadClass("com.baidu.tieba.R$id").getField("right_textview").getInt(null));
                TextView textView = new TextView(activity);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(60, 0, 20, 0);
                textView.setLayoutParams(layoutParams);
                textView.setTextSize(DisplayHelper.px2Dip(activity, edit.getTextSize()));
                textView.setTextColor(edit.getTextColors());
                textView.setText("搜索");
                LinearLayout naviRightButton = activity.findViewById(sClassLoader.loadClass("com.baidu.tieba.R$id").getField("navi_right_button").getInt(null));
                naviRightButton.addView(textView);
                textView.setOnClickListener(v -> showRegexDialog(activity));
            }
        });
        Rule.findRule(sRes.getString(R.string.ThreadStore), new Rule.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, Boolean[].class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        for (Field field : param.getResult().getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.get(param.getResult()) instanceof ArrayList) {
                                ArrayList<?> arrayList = (ArrayList<?>) field.get(param.getResult());
                                if (arrayList == null) return;
                                final Pattern pattern = Pattern.compile(mRegex);
                                label:
                                for (int j = 0; j < arrayList.size(); j++) {
                                    // com.baidu.tbadk.baseEditMark.MarkData
                                    String[] strings = new String[]{(String) XposedHelpers.getObjectField(arrayList.get(j), "mTitle"),
                                            (String) XposedHelpers.getObjectField(arrayList.get(j), "mForumName"),
                                            (String) XposedHelpers.getObjectField(arrayList.get(j), "mAuthorName")};
                                    for (String string : strings) {
                                        if (pattern.matcher(string).find()) {
                                            continue label;
                                        }
                                    }
                                    arrayList.remove(j);
                                    j--;
                                }
                                for (int j = 0; j < arrayList.size(); j++) {
                                    XposedHelpers.setObjectField(arrayList.get(j), "mAuthorName", String.format("%s-%s",
                                            XposedHelpers.getObjectField(arrayList.get(j), "mForumName"), XposedHelpers.getObjectField(arrayList.get(j), "mAuthorName")));
                                }
                                return;
                            }
                        }
                    }
                });
            }
        });
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
        TbDialog bdAlert = new TbDialog(sClassLoader, activity, sRes, null, null, true, editText);
        bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
        bdAlert.setOnYesButtonClickListener(v -> {
            try {
                Pattern.compile(editText.getText().toString());
                activity.finish();
                activity.startActivity(activity.getIntent());
                bdAlert.dismiss();
            } catch (PatternSyntaxException e) {
                TbToast.showTbToast(sClassLoader, activity, sRes, e.getMessage(), TbToast.LENGTH_SHORT);
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
