package gm.tieba.tabswitch.hookImpl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
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
                            showNoteDialog(activity, (String) tailContainer.getTag());
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
    private static void showNoteDialog(Activity activity, String key) {
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
        AlertDialog alertDialog;
        if (DisplayHelper.isLightMode(activity))
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle("备注").setView(editText).setCancelable(true)
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("确定", (dialogInterface, i) -> {
                    }).create();
        else alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                .setTitle("备注").setView(editText).setCancelable(true)
                .setNegativeButton("取消", (dialogInterface, i) -> {
                }).setPositiveButton("确定", (dialogInterface, i) -> {
                }).create();
        alertDialog.show();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            SharedPreferences.Editor editor = tsNotes.edit();
            if ("".equals(editText.getText().toString())) editor.putString(key, null);
            else editor.putString(key, editText.getText().toString());
            editor.commit();
            activity.finish();
            activity.startActivity(activity.getIntent());
            alertDialog.dismiss();
        });
    }
}