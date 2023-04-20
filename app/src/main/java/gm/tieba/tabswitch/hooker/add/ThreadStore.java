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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;
import gm.tieba.tabswitch.widget.NavigationBar;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbEditText;
import gm.tieba.tabswitch.widget.TbToast;

public class ThreadStore extends XposedContext implements IHooker, Obfuscated {

    @NonNull
    @Override
    public String key() {
        return "thread_store";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(new StringMatcher("c/f/post/threadstore"));
    }

    private String mRegex = "";

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.myCollection.CollectTabActivity", sClassLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        final var controller = ReflectUtils.getObjectField(param.thisObject, 1);
                        final var activity = (Activity) param.thisObject;
                        new NavigationBar(controller)
                                .addTextButton("搜索", v -> showRegexDialog(activity));
                    }
                });
        AcRules.findRule(matchers(), (matcher, clazz, method) ->
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, Boolean[].class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        final var list = ReflectUtils.getObjectField(param.getResult(), ArrayList.class);
                        if (list == null) return;
                        final Pattern pattern = Pattern.compile(mRegex);
                        list.removeIf(o -> {
                            // com.baidu.tbadk.baseEditMark.MarkData
                            final String[] strings = new String[]{(String) XposedHelpers.getObjectField(o, "mTitle"),
                                    (String) XposedHelpers.getObjectField(o, "mForumName"),
                                    (String) XposedHelpers.getObjectField(o, "mAuthorName")};
                            for (final String string : strings) {
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
