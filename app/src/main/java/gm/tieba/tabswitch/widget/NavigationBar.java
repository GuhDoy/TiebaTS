package gm.tieba.tabswitch.widget;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.Reflect;

public class NavigationBar {
    private final ClassLoader mClassLoader;
    public Class<?> mClass;
    private Object mNavigationBar;
    private final boolean mIsLightMode;

    public NavigationBar(ClassLoader classLoader, Activity activity, Object instanceInClass) {
        mClassLoader = classLoader;
        mIsLightMode = DisplayHelper.isLightMode(activity);
        try {
            mClass = classLoader.loadClass("com.baidu.tbadk.core.view.NavigationBar");
            mNavigationBar = Reflect.getObjectField(instanceInClass,
                    "com.baidu.tbadk.core.view.NavigationBar");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void addTextButton(String text, View.OnClickListener onClick) {
        try {
            Class<?> ControlAlign = mClassLoader.loadClass(
                    "com.baidu.tbadk.core.view.NavigationBar$ControlAlign");
            for (Object HORIZONTAL_RIGHT : ControlAlign.getEnumConstants()) {
                if (HORIZONTAL_RIGHT.toString().equals("HORIZONTAL_RIGHT")) {
                    TextView textView = (TextView) mClass.getDeclaredMethod("addTextButton",
                            ControlAlign, String.class, View.OnClickListener.class)
                            .invoke(mNavigationBar, HORIZONTAL_RIGHT, text, onClick);
                    if (!mIsLightMode) {
                        textView.setTextColor(Color.parseColor("#FFCBCBCC"));
                    }
                    break;
                }
            }
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    public void setTitleText(String title) {
        try {
            mClass.getDeclaredMethod("setTitleText", String.class).invoke(mNavigationBar, title);
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }
}
