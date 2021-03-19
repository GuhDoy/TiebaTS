package gm.tieba.tabswitch.hookImpl;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.util.DisplayHelper;

public class ThreadStore extends Hook {
    private static String regex = "";

    public static void hook(ClassLoader classLoader) throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.myCollection.CollectTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                TextView edit = activity.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("right_textview").getInt(null));
                TextView textView = new TextView(activity);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(60, 0, 20, 0);
                textView.setLayoutParams(layoutParams);
                textView.setTextSize(DisplayHelper.px2Dip(activity, edit.getTextSize()));
                textView.setTextColor(edit.getTextColors());
                textView.setText("搜索");
                LinearLayout naviRightButton = activity.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("navi_right_button").getInt(null));
                naviRightButton.addView(textView);
                textView.setOnClickListener(v -> showRegexDialog(classLoader, activity));
            }
        });
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            if (Objects.equals(map.get("rule"), "\"c/f/post/threadstore\""))
                XposedHelpers.findAndHookMethod(map.get("class"), classLoader, map.get("method"), Boolean[].class, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Field[] fields = param.getResult().getClass().getDeclaredFields();
                        for (Field field : fields) {
                            field.setAccessible(true);
                            if (field.get(param.getResult()) instanceof ArrayList) {
                                ArrayList<?> arrayList = (ArrayList<?>) field.get(param.getResult());
                                if (arrayList == null) return;
                                label:
                                for (int j = 0; j < arrayList.size(); j++) {
                                    // com.baidu.tbadk.baseEditMark.MarkData
                                    String[] strings = new String[]{(String) XposedHelpers.getObjectField(arrayList.get(j), "mTitle"),
                                            (String) XposedHelpers.getObjectField(arrayList.get(j), "mForumName"),
                                            (String) XposedHelpers.getObjectField(arrayList.get(j), "mAuthorName")};
                                    for (String string : strings)
                                        if (Pattern.compile(regex).matcher(string).find())
                                            continue label;
                                    arrayList.remove(j);
                                    j--;
                                }
                                for (int j = 0; j < arrayList.size(); j++)
                                    XposedHelpers.setObjectField(arrayList.get(j), "mAuthorName", String.format("%s-%s",
                                            XposedHelpers.getObjectField(arrayList.get(j), "mForumName"), XposedHelpers.getObjectField(arrayList.get(j), "mAuthorName")));
                                return;
                            }
                        }
                    }
                });
        }
    }

    private static void showRegexDialog(ClassLoader classLoader, Activity activity) {
        EditText editText = new TSPreferenceHelper.TbEditTextBuilder(classLoader, activity).editText;
        editText.setHint("请输入正则表达式，如.*");
        editText.setText(regex);
        editText.selectAll();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                regex = s.toString();
            }
        });
        TSPreferenceHelper.TbDialogBuilder bdalert = new TSPreferenceHelper.TbDialogBuilder(classLoader, activity, null, null, true, editText);
        bdalert.setOnNoButtonClickListener(v -> bdalert.dismiss());
        bdalert.setOnYesButtonClickListener(v -> {
            try {
                Pattern.compile(editText.getText().toString());
                activity.finish();
                activity.startActivity(activity.getIntent());
                bdalert.dismiss();
            } catch (PatternSyntaxException e) {
                Toast.makeText(activity, Log.getStackTraceString(e), Toast.LENGTH_SHORT).show();
            }
        });
        bdalert.show();
        bdalert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
}