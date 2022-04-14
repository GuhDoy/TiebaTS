package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.ReflectUtils;
import gm.tieba.tabswitch.widget.NavigationBar;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbEditText;
import gm.tieba.tabswitch.widget.TbToast;

public class ThreadStore extends XposedContext implements IHooker {
    private String mRegex = "";

    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.myCollection.CollectTabActivity", sClassLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        var controller = ReflectUtils.getObjectField(param.thisObject, 1);
                        var activity = (Activity) param.thisObject;
                        new NavigationBar(controller)
                                .addTextButton("搜索", v -> showRegexDialog(activity));
                    }
                });
        AcRules.findRule(Constants.getMatchers().get(ThreadStore.class), (AcRules.Callback) (matcher, clazz, method) ->
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, Boolean[].class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ArrayList<?> list = (ArrayList<?>) ReflectUtils.getObjectField(param.getResult(), ArrayList.class);
                        if (list == null) return;
                        final Pattern pattern = Pattern.compile(mRegex);
                        list.removeIf(o -> {
                            // com.baidu.tbadk.baseEditMark.MarkData
                            String[] strings = new String[]{(String) XposedHelpers.getObjectField(o, "mTitle"),
                                    (String) XposedHelpers.getObjectField(o, "mForumName"),
                                    (String) XposedHelpers.getObjectField(o, "mAuthorName")};
                            for (String string : strings) {
                                if (pattern.matcher(string).find()) {
                                    return false;
                                }
                            }
                            return true;
                        });
                        for (int j = 0; j < list.size(); j++) {
                            XposedHelpers.setObjectField(list.get(j), "mAuthorName", String.format("%s - %s",
                                    XposedHelpers.getObjectField(list.get(j), "mForumName"),
                                    XposedHelpers.getObjectField(list.get(j), "mAuthorName")));
                        }
                    }
                }));
    }

    private void showRegexDialog(Activity activity) {
        EditText editText = new TbEditText(activity);
        editText.setHint(Constants.getStrings().get("regex_hint"));
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
