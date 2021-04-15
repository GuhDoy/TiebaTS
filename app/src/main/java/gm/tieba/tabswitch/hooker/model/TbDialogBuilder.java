package gm.tieba.tabswitch.hooker.model;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
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
import gm.tieba.tabswitch.util.DisplayHelper;

public class TbDialogBuilder {
    private final ClassLoader mClassLoader;
    private Class<?> mTbDialog;
    private Object mBdAlert;
    private Object mPageContext;
    private ViewGroup mRootView;
    private AlertDialog mDialog;

    public TbDialogBuilder(ClassLoader classLoader, Context context, String title, String message, boolean cancelable, View contentView) {
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.BaseFragment", classLoader, "getPageContext", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mPageContext = param.getResult();
            }
        });
        mClassLoader = classLoader;
        try {
            Rule.findRule(new Rule.RuleCallBack() {
                @Override
                public void onRuleFound(String rule, String clazz, String method) {
                    try {
                        mTbDialog = classLoader.loadClass(clazz);
                        mBdAlert = mTbDialog.getConstructor(Activity.class).newInstance((Activity) context);
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
                        if (DisplayHelper.isLightMode(context)) {
                            mRootView.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("bdDialog_divider_line").getInt(null)).setBackgroundColor(mRootView.getContext().getColor(classLoader.loadClass("com.baidu.tieba.R$color").getField("CAM_X0204").getInt(null)));
                            mRootView.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("divider_yes_no_button").getInt(null)).setBackgroundColor(mRootView.getContext().getColor(classLoader.loadClass("com.baidu.tieba.R$color").getField("CAM_X0204").getInt(null)));
                        } else {
                            mRootView.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("bdDialog_divider_line").getInt(null)).setBackgroundColor(mRootView.getContext().getColor(classLoader.loadClass("com.baidu.tieba.R$color").getField("CAM_X0105").getInt(null)));
                            mRootView.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("divider_yes_no_button").getInt(null)).setBackgroundColor(mRootView.getContext().getColor(classLoader.loadClass("com.baidu.tieba.R$color").getField("CAM_X0105").getInt(null)));
                        }
                    } catch (Throwable throwable) {
                        XposedBridge.log(throwable);
                    }
                }
            }, "Lcom/baidu/tieba/R$layout;->dialog_bdalert:I");
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
    }

    public void setOnNoButtonClickListener(View.OnClickListener onClickListener) {
        try {
            try {
                XposedHelpers.setObjectField(mBdAlert, "mNegativeButtonTip", "取消");
            } catch (NoSuchFieldError e) {
                XposedHelpers.setObjectField(mBdAlert, "m", "取消");
            }
            mRootView.findViewById(mClassLoader.loadClass("com.baidu.tieba.R$id").getField("no").getInt(null)).setOnClickListener(onClickListener);
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
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
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
    }

    public TextView getYesButton() {
        try {
            return mRootView.findViewById(mClassLoader.loadClass("com.baidu.tieba.R$id").getField("yes").getInt(null));
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
        return null;
    }

    public void show() {
        try {
            for (Method method : mTbDialog.getDeclaredMethods()) {
                if (Arrays.toString(method.getParameterTypes()).startsWith("[interface") && !Arrays.toString(method.getParameterTypes()).contains("$")) {
                    method.invoke(mBdAlert, mPageContext);// create
                }
            }
            LinearLayout parent = mRootView.findViewById(mClassLoader.loadClass("com.baidu.tieba.R$id").getField("dialog_content").getInt(null));
            if (!DisplayHelper.isLightMode(mRootView.getContext()) && parent.getChildAt(0) instanceof LinearLayout) {
                LinearLayout linearLayout = (LinearLayout) parent.getChildAt(0);
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    View view = linearLayout.getChildAt(i);
                    if (view instanceof TextView) {
                        ((TextView) view).setTextColor(Color.parseColor("#FFCBCBCC"));
                    }
                }
            }
            for (Method method : mTbDialog.getDeclaredMethods()) {
                if (Arrays.toString(method.getParameterTypes()).equals("[]") && Objects.equals(method.getReturnType(), mTbDialog)) {
                    method.invoke(mBdAlert);// show
                    break;
                }
            }
            try {
                mDialog = (AlertDialog) XposedHelpers.getObjectField(mBdAlert, "mDialog");
            } catch (NoSuchFieldError e) {
                mDialog = (AlertDialog) XposedHelpers.getObjectField(mBdAlert, "w");
            }
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
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
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
    }
}