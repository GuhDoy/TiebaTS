package gm.tieba.tabswitch.widget;

import android.view.View;
import android.widget.TextView;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.util.Reflect;

public class NavigationBar extends BaseHooker {
    public Class<?> mClass;
    private Object mNavigationBar;

    public NavigationBar(Object instanceInClass) {
        try {
            mClass = sClassLoader.loadClass("com.baidu.tbadk.core.view.NavigationBar");
            mNavigationBar = Reflect.getObjectField(instanceInClass,
                    "com.baidu.tbadk.core.view.NavigationBar");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void addTextButton(String text, View.OnClickListener onClick) {
        try {
            Class<?> ControlAlign = sClassLoader.loadClass(
                    "com.baidu.tbadk.core.view.NavigationBar$ControlAlign");
            for (Object HORIZONTAL_RIGHT : ControlAlign.getEnumConstants()) {
                if (HORIZONTAL_RIGHT.toString().equals("HORIZONTAL_RIGHT")) {
                    TextView textView = (TextView) mClass.getDeclaredMethod("addTextButton",
                            ControlAlign, String.class, View.OnClickListener.class)
                            .invoke(mNavigationBar, HORIZONTAL_RIGHT, text, onClick);
                    textView.setTextColor(Reflect.getColor("CAM_X0105"));
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
