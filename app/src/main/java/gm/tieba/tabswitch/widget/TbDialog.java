package gm.tieba.tabswitch.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.XposedWrapper;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.ReflectUtils;

public class TbDialog extends XposedWrapper {
    private Class<?> mClass;
    private Object mBdAlert;
    private Object mPageContext;
    private ViewGroup mRootView;
    private AlertDialog mDialog;

    public TbDialog(Activity activity, String title, String message, boolean cancelable, View contentView) {
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.BaseFragment", sClassLoader,
                "getPageContext", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mPageContext = param.getResult();
                    }
                });
        AcRules.findRule(sRes.getString(R.string.TbDialog), (AcRules.Callback) (rule, clazz, method) -> {
            mClass = XposedHelpers.findClass(clazz, sClassLoader);
            mBdAlert = XposedHelpers.newInstance(mClass, activity);
            try {
                mRootView = (ViewGroup) XposedHelpers.getObjectField(mBdAlert, "mRootView");
                XposedHelpers.setObjectField(mBdAlert, "mTitle", title);
                XposedHelpers.setObjectField(mBdAlert, "mMessage", message);
                XposedHelpers.setObjectField(mBdAlert, "mCancelable", cancelable);
                XposedHelpers.setObjectField(mBdAlert, "mContentView", contentView);
            } catch (NoSuchFieldError e) {
                mRootView = (ViewGroup) XposedHelpers.getObjectField(mBdAlert, "y");
                XposedHelpers.setObjectField(mBdAlert, "f", title);
                XposedHelpers.setObjectField(mBdAlert, "h", message);
                XposedHelpers.setObjectField(mBdAlert, "C", cancelable);
                XposedHelpers.setObjectField(mBdAlert, "g", contentView);
            }
            int color = ReflectUtils.getColor("CAM_X0204");
            mRootView.findViewById(ReflectUtils.getId("bdDialog_divider_line"))
                    .setBackgroundColor(color);
            mRootView.findViewById(ReflectUtils.getId("divider_yes_no_button"))
                    .setBackgroundColor(color);
        });
    }

    public void setOnNoButtonClickListener(View.OnClickListener l) {
        String cancel = getContext().getString(android.R.string.cancel);
        try {
            XposedHelpers.setObjectField(mBdAlert, "mNegativeButtonTip", cancel);
        } catch (NoSuchFieldError e) {
            XposedHelpers.setObjectField(mBdAlert, "m", cancel);
        }
        mRootView.findViewById(ReflectUtils.getId("no")).setOnClickListener(l);
    }

    public void setOnYesButtonClickListener(View.OnClickListener l) {
        String ok = getContext().getString(android.R.string.ok);
        try {
            XposedHelpers.setObjectField(mBdAlert, "mPositiveButtonTip", ok);
        } catch (NoSuchFieldError e) {
            XposedHelpers.setObjectField(mBdAlert, "l", ok);
        }
        findYesButton().setOnClickListener(l);
    }

    public TextView findYesButton() {
        return mRootView.findViewById(ReflectUtils.getId("yes"));
    }

    public void show() {
        for (Method method : mClass.getDeclaredMethods()) {
            if (Arrays.toString(method.getParameterTypes()).startsWith("[interface")
                    && !Arrays.toString(method.getParameterTypes()).contains("$")) {
                ReflectUtils.callMethod(method, mBdAlert, mPageContext);// create
            }
        }
        LinearLayout parent = mRootView.findViewById(ReflectUtils.getId("dialog_content"));
        if (parent.getChildAt(0) instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) parent.getChildAt(0);
            for (int i = 0; i < linearLayout.getChildCount(); i++) {
                View view = linearLayout.getChildAt(i);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(ReflectUtils.getColor("CAM_X0105"));
                }
            }
        }
        for (Method method : mClass.getDeclaredMethods()) {
            if (Arrays.toString(method.getParameterTypes()).equals("[]")
                    && Objects.equals(method.getReturnType(), mClass)) {
                ReflectUtils.callMethod(method, mBdAlert);// show
                break;
            }
        }
        try {
            mDialog = (AlertDialog) XposedHelpers.getObjectField(mBdAlert, "mDialog");
        } catch (NoSuchFieldError e) {
            mDialog = (AlertDialog) XposedHelpers.getObjectField(mBdAlert, "w");
        }
    }

    public Window getWindow() {
        if (mDialog == null) throw new IllegalStateException("you must call show before getWindow");
        else return mDialog.getWindow();
    }

    public void dismiss() {
        try {
            XposedHelpers.callMethod(mBdAlert, "dismiss");
        } catch (NoSuchMethodError e) {
            XposedHelpers.callMethod(mBdAlert, "k");
        }
    }
}
