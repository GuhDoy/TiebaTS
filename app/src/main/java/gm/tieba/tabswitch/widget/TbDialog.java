package gm.tieba.tabswitch.widget;

import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.util.ReflectUtils;

public class TbDialog extends XposedContext implements Obfuscated {
    private Class<?> mClass;
    private Object mBdAlert;
    private Object mPageContext;
    private Object mDialog;

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(new StringMatcher("Dialog must be created by function create()!"));
    }

    public TbDialog() {
    }

    public TbDialog(final Activity activity, final String title, final String message, final boolean cancelable, final View contentView) {
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.core.BaseFragment", sClassLoader,
                "getPageContext", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        mPageContext = param.getResult();
                    }
                });
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            final var cls = XposedHelpers.findClass(clazz, sClassLoader);
            if (cls.getDeclaredMethods().length < 20) {
                return;
            }
            mClass = cls;
            mBdAlert = XposedHelpers.newInstance(mClass, activity);
            try {
                XposedHelpers.setObjectField(mBdAlert, "mTitle", title);
                XposedHelpers.setObjectField(mBdAlert, "mMessage", message);
                XposedHelpers.setObjectField(mBdAlert, "mCancelable", cancelable);
                XposedHelpers.setObjectField(mBdAlert, "mContentView", contentView);
            } catch (final NoSuchFieldError e) {
                XposedHelpers.setObjectField(mBdAlert, "f", title);
                XposedHelpers.setObjectField(mBdAlert, "h", message);
                XposedHelpers.setObjectField(mBdAlert, "C", cancelable);
                XposedHelpers.setObjectField(mBdAlert, "g", contentView);
            }

            initButtonStyle(param -> {
                final int color = ReflectUtils.getColor("CAM_X0204");
                // R.id.bdDialog_divider_line
                final var bdDialogDividerLine = (View) XposedHelpers.getObjectField(mBdAlert, "bdDialog_divider_line");
                if (bdDialogDividerLine != null) {
                    bdDialogDividerLine.setBackgroundColor(color);
                }
                // R.id.divider_yes_no_button
                final var dividerWithButton = (View) XposedHelpers.getObjectField(mBdAlert, "dividerWithButton");
                if (dividerWithButton != null) {
                    dividerWithButton.setBackgroundColor(color);
                }
            });
        });
    }

    // called in create()
    private void initButtonStyle(final Consumer<XC_MethodHook.MethodHookParam> consumer) {
        XposedHelpers.findAndHookMethod(mClass, "initButtonStyle", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                consumer.accept(param);
            }
        });
    }

    public void setOnNoButtonClickListener(final View.OnClickListener l) {
        initButtonStyle(param -> {
            final var cancel = getContext().getString(android.R.string.cancel);
            try {
                XposedHelpers.setObjectField(mBdAlert, "mNegativeButtonTip", cancel);
            } catch (final NoSuchFieldError e) {
                XposedHelpers.setObjectField(mBdAlert, "m", cancel);
            }
            // R.id.no
            final var noButton = (TextView) XposedHelpers.getObjectField(mBdAlert, "noButton");
            if (noButton != null) {
                noButton.setOnClickListener(l);
            }
        });
    }

    public void setOnYesButtonClickListener(final View.OnClickListener l) {
        initButtonStyle(param -> {
            final var ok = getContext().getString(android.R.string.ok);
            try {
                XposedHelpers.setObjectField(mBdAlert, "mPositiveButtonTip", ok);
            } catch (final NoSuchFieldError e) {
                XposedHelpers.setObjectField(mBdAlert, "l", ok);
            }
            final var yesButton = (TextView) findYesButton();
            if (yesButton != null) {
                yesButton.setOnClickListener(l);
            }
        });
    }

    public TextView findYesButton() {
        // R.id.yes
        return (TextView) XposedHelpers.getObjectField(mBdAlert, "yesButton");
    }

    public void show() {
        for (final var md : mClass.getDeclaredMethods()) {
            final var parameterTypesString = Arrays.toString(md.getParameterTypes());
            if (parameterTypesString.startsWith("[interface") &&
                    !parameterTypesString.contains("$")) {
                ReflectUtils.callMethod(md, mBdAlert, mPageContext); // create()
            }
        }
//        LinearLayout parent = mRootView.findViewById(ReflectUtils.getId("dialog_content"));
//        if (parent.getChildAt(0) instanceof LinearLayout) {
//            LinearLayout linearLayout = (LinearLayout) parent.getChildAt(0);
//            for (int i = 0, childCount = linearLayout.getChildCount(); i < childCount; i++) {
//                View view = linearLayout.getChildAt(i);
//                if (view instanceof TextView) {
//                    ((TextView) view).setTextColor(ReflectUtils.getColor("CAM_X0105"));
//                }
//            }
//        }
        for (final var method : mClass.getDeclaredMethods()) {
            if (method.getParameterTypes().length == 0 && mClass.equals(method.getReturnType())) {
                ReflectUtils.callMethod(method, mBdAlert); // show()
                break;
            }
        }
        try {
            mDialog = XposedHelpers.getObjectField(mBdAlert, "mDialog");
        } catch (final NoSuchFieldError e) {
            mDialog = XposedHelpers.getObjectField(mBdAlert, "w");
        }
    }

    public Window getWindow() {
        if (mDialog == null) throw new IllegalStateException("you must call show before getWindow");
        else return (Window) XposedHelpers.callMethod(mDialog, "getWindow");
    }

    public void dismiss() {
        try {
            XposedHelpers.callMethod(mBdAlert, "dismiss");
        } catch (final NoSuchMethodError e) {
            XposedHelpers.callMethod(mBdAlert, "k");
        }
    }
}
