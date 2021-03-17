package gm.tieba.tabswitch.hookImpl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.Reflect;

public class MyAttention extends Hook {
    public static void hook(ClassLoader classLoader, Context context) throws Throwable {
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            if (Objects.equals(map.get("rule"), "Lcom/baidu/tieba/R$layout;->person_list_item:I"))
                XposedHelpers.findAndHookMethod(map.get("class"), classLoader, map.get("method"), int.class, View.class, ViewGroup.class, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) Reflect.getObjectField(param.thisObject, "com.baidu.tieba.myAttentionAndFans.PersonListActivity");
                        View root = (View) param.getResult();
                        View itemView = root.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("item_view").getInt(null));
                        TextView textView = root.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("name").getInt(null));
                        View tailContainer = root.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("tail_container").getInt(null));
                        tailContainer.setTag(textView.getText());
                        itemView.setOnLongClickListener(v -> {
                            showNoteDialog(classLoader, activity, (String) tailContainer.getTag());
                            return false;
                        });
                        SharedPreferences tsNotes = activity.getSharedPreferences("TS_notes", Context.MODE_PRIVATE);
                        if (tsNotes.getString((String) textView.getText(), null) != null)
                            textView.setText(String.format("%s(%s)", tsNotes.getString((String) textView.getText(), null), textView.getText()));
                    }
                });
        }
        XposedHelpers.findAndHookMethod("tbclient.User$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                SharedPreferences tsNotes = context.getSharedPreferences("TS_notes", Context.MODE_PRIVATE);
                String nameShow = (String) XposedHelpers.getObjectField(param.thisObject, "name_show");
                if (tsNotes.getString(nameShow, null) != null)
                    XposedHelpers.setObjectField(param.thisObject, "name_show", String.format("%s(%s)",
                            tsNotes.getString(nameShow, null), nameShow));
            }
        });
    }

    @SuppressLint("ApplySharedPref")
    private static void showNoteDialog(ClassLoader classLoader, Activity activity, String key) {
        SharedPreferences tsNotes = activity.getSharedPreferences("TS_notes", Context.MODE_PRIVATE);
        EditText editText = new EditText(activity);
        editText.setHint(key);
        if (tsNotes.getString(key, null) != null)
            editText.setText(tsNotes.getString(key, null));
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
        TSPreferenceHelper.TbDialogBuilder bdalert = new TSPreferenceHelper.TbDialogBuilder(classLoader, activity, null, null, editText);
        bdalert.setOnNoButtonClickListener(v -> bdalert.dismiss());
        bdalert.setOnYesButtonClickListener(v -> {
            SharedPreferences.Editor editor = tsNotes.edit();
            if (TextUtils.isEmpty(editText.getText())) editor.putString(key, null);
            else editor.putString(key, editText.getText().toString());
            editor.commit();
            activity.finish();
            activity.startActivity(activity.getIntent());
            bdalert.dismiss();
        });
        bdalert.show();
        bdalert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
}