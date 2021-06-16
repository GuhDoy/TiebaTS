package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.Collections;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.dao.Adp;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.TSPreferenceHelper;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbEditText;

public class MyAttention extends BaseHooker implements IHooker {
    public static LinearLayout createNotesPreference(Activity activity) {
        Preferences.putBoolean("my_attention", !Preferences.getNotes().isEmpty());
        TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        Set<String> follows = Collections.emptySet();
        try {
            follows = Adp.getInstance().parseDatabase().follows;
            preferenceLayout.addView(TSPreferenceHelper.createTextView(null));
        } catch (Throwable e) {
            XposedBridge.log(e);
            preferenceLayout.addView(TSPreferenceHelper.createTextView("读取数据库缓存失败\n"
                    + Log.getStackTraceString(e)));
        }
        for (String follow : follows) {
            preferenceLayout.addView(TSPreferenceHelper.createButton(follow, Preferences.getNote(follow),
                    v -> showNoteDialog(activity, follow)));
        }

        boolean isAdd = true;
        for (String follow : Preferences.getNotes().keySet()) {
            if (follows.contains(follow)) continue;
            if (isAdd) {
                preferenceLayout.addView(TSPreferenceHelper.createTextView("已取消关注的人"));
                isAdd = false;
            }
            preferenceLayout.addView(TSPreferenceHelper.createButton(follow,
                    Preferences.getNote(follow), v -> showNoteDialog(activity, follow)));
        }
        return preferenceLayout;
    }

    private static void showNoteDialog(Activity activity, String name) {
        EditText editText = new TbEditText(activity);
        String note = Preferences.getNote(name);
        editText.setText(note != null ? note : name);
        TbDialog bdAlert = new TbDialog(activity, null, null, true, editText);
        bdAlert.setOnNoButtonClickListener(v -> bdAlert.dismiss());
        bdAlert.setOnYesButtonClickListener(v -> {
            SharedPreferences.Editor editor = Preferences.getTsNotesEditor();
            if (TextUtils.isEmpty(editText.getText()) || editText.getText().toString().equals(name)) {
                editor.remove(name);
            } else editor.putString(name, editText.getText().toString());
            editor.commit();
            bdAlert.dismiss();
            activity.recreate();
        });
        bdAlert.show();
        bdAlert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        editText.setSingleLine();
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                bdAlert.getYesButton().performClick();
                return true;
            }
            return false;
        });
        editText.selectAll();
        editText.requestFocus();
    }

    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.User$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String nameShow = (String) XposedHelpers.getObjectField(param.thisObject, "name_show");
                if (Preferences.getNote(nameShow) != null) {
                    XposedHelpers.setObjectField(param.thisObject, "name_show",
                            String.format("%s(%s)", Preferences.getNote(nameShow), nameShow));
                }
            }
        });
    }
}
