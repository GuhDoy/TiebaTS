package gm.tieba.tabswitch.hookImpl;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.Reflect;

public class HistoryCache extends Hook {
    private static String regex = "";

    public static void hook(ClassLoader classLoader) throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.myCollection.history.PbHistoryActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Object mNavigationBar = Reflect.getObjectField(param.thisObject, "com.baidu.tbadk.core.view.NavigationBar");
                Class<?> ControlAlign = classLoader.loadClass("com.baidu.tbadk.core.view.NavigationBar$ControlAlign");
                Object[] enums = ControlAlign.getEnumConstants();
                for (Object HORIZONTAL_RIGHT : enums)
                    if (HORIZONTAL_RIGHT.toString().equals("HORIZONTAL_RIGHT")) {
                        Class<?> NavigationBar = classLoader.loadClass("com.baidu.tbadk.core.view.NavigationBar");
                        TextView textView = (TextView) NavigationBar.getDeclaredMethod("addTextButton", ControlAlign, String.class, View.OnClickListener.class)
                                .invoke(mNavigationBar, HORIZONTAL_RIGHT, "搜索", (View.OnClickListener) v -> showRegexDialog(classLoader, activity));
                        if (DisplayHelper.isLightMode(activity))
                            textView.setTextColor(Color.parseColor("#FF3E3D40"));
                        else textView.setTextColor(Color.parseColor("#FFCBCBCC"));
                        return;
                    }
            }
        });
        Method[] methods = classLoader.loadClass("com.baidu.tieba.myCollection.history.PbHistoryActivity").getDeclaredMethods();
        for (Method method : methods)
            if (Arrays.toString(method.getParameterTypes()).equals("[interface java.util.List]"))
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        List<?> list = (List<?>) param.args[0];
                        if (list == null) return;
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
                            for (String string : strings)
                                if (Pattern.compile(regex).matcher(string).find())
                                    continue label;
                            list.remove(j);
                            j--;
                        }
                    }
                });
    }

    private static void showRegexDialog(ClassLoader classLoader, Activity activity) {
        EditText editText = new EditText(activity);
        editText.setHint("请输入正则表达式，如.*");
        editText.setText(regex);
        editText.selectAll();
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setTextSize(18);
        editText.requestFocus();
        editText.setHintTextColor(Hook.modRes.getColor(R.color.colorProgress, null));
        if (!DisplayHelper.isLightMode(activity))
            editText.setTextColor(modRes.getColor(R.color.colorPrimary, null));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(layoutParams);
        try {
            editText.setBackgroundResource(classLoader.loadClass("com.baidu.tieba.R$drawable").getField("blue_rectangle_input_bg").getInt(null));
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
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
        TSPreferenceHelper.TbDialogBuilder bdalert = new TSPreferenceHelper.TbDialogBuilder(classLoader, activity, null, null, editText);
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