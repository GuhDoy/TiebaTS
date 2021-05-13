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
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.Rule;
import gm.tieba.tabswitch.util.Reflect;

public class TbDialog extends BaseHooker {
    private Class<?> mTbDialog;
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
        try {
            Rule.findRule(sRes.getString(R.string.TbDialog), new Rule.Callback() {
                @Override
                public void onRuleFound(String rule, String clazz, String method) {
                    try {
                        mTbDialog = sClassLoader.loadClass(clazz);
                        mBdAlert = mTbDialog.getConstructor(Activity.class).newInstance((Activity) activity);
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
                        int color = Reflect.getColor("CAM_X0204");
                        mRootView.findViewById(Reflect.getId("bdDialog_divider_line"))
                                .setBackgroundColor(color);
                        mRootView.findViewById(Reflect.getId("divider_yes_no_button"))
                                .setBackgroundColor(color);
                    } catch (Throwable e) {
                        XposedBridge.log(e);
                    }
                }
            });
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    public void setOnNoButtonClickListener(View.OnClickListener onClickListener) {
        try {
            try {
                XposedHelpers.setObjectField(mBdAlert, "mNegativeButtonTip", "取消");
            } catch (NoSuchFieldError e) {
                XposedHelpers.setObjectField(mBdAlert, "m", "取消");
            }
            mRootView.findViewById(Reflect.getId("no")).setOnClickListener(onClickListener);
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    public void setOnYesButtonClickListener(View.OnClickListener onClickListener) {
        try {
            try {
                XposedHelpers.setObjectField(mBdAlert, "mPositiveButtonTip", "确定");
            } catch (NoSuchFieldError e) {
                XposedHelpers.setObjectField(mBdAlert, "l", "确定");
            }
            getYesButton().setOnClickListener(onClickListener);
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    public TextView getYesButton() {
        try {
            return mRootView.findViewById(Reflect.getId("yes"));
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
        return null;
    }

    public void show() {
        try {
            for (Method method : mTbDialog.getDeclaredMethods()) {
                if (Arrays.toString(method.getParameterTypes()).startsWith("[interface")
                        && !Arrays.toString(method.getParameterTypes()).contains("$")) {
                    method.invoke(mBdAlert, mPageContext);// create
                }
            }
            LinearLayout parent = mRootView.findViewById(Reflect.getId("dialog_content"));
            if (parent.getChildAt(0) instanceof LinearLayout) {
                LinearLayout linearLayout = (LinearLayout) parent.getChildAt(0);
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    View view = linearLayout.getChildAt(i);
                    if (view instanceof TextView) {
                        ((TextView) view).setTextColor(Reflect.getColor("CAM_X0105"));
                    }
                }
            }
            for (Method method : mTbDialog.getDeclaredMethods()) {
                if (Arrays.toString(method.getParameterTypes()).equals("[]")
                        && Objects.equals(method.getReturnType(), mTbDialog)) {
                    method.invoke(mBdAlert);// show
                    break;
                }
            }
            try {
                mDialog = (AlertDialog) XposedHelpers.getObjectField(mBdAlert, "mDialog");
            } catch (NoSuchFieldError e) {
                mDialog = (AlertDialog) XposedHelpers.getObjectField(mBdAlert, "w");
            }
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    public Window getWindow() {
        return mDialog.getWindow();
    }

    public void dismiss() {
        try {
            try {
                mTbDialog.getDeclaredMethod("dismiss").invoke(mBdAlert);
            } catch (NoSuchMethodException e) {
                mTbDialog.getDeclaredMethod("k").invoke(mBdAlert);
            }
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }
}
