package gm.tieba.tabswitch.widget;

import android.view.View;
import android.widget.TextView;

import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.util.ReflectUtils;

public class NavigationBar extends BaseHooker {
    private Object mNavigationBar;

    public NavigationBar(Object thisObject) {
        mNavigationBar = ReflectUtils.getObjectField(thisObject,
                "com.baidu.tbadk.core.view.NavigationBar");
    }

    public void addTextButton(String text, View.OnClickListener l) {
        Class<?> ControlAlign = XposedHelpers.findClass(
                "com.baidu.tbadk.core.view.NavigationBar$ControlAlign", sClassLoader);
        for (Object HORIZONTAL_RIGHT : ControlAlign.getEnumConstants()) {
            if (HORIZONTAL_RIGHT.toString().equals("HORIZONTAL_RIGHT")) {
                TextView textView = (TextView) XposedHelpers.callMethod(mNavigationBar,
                        "addTextButton", HORIZONTAL_RIGHT, text, l);
                textView.setTextColor(ReflectUtils.getColor("CAM_X0105"));
                break;
            }
        }
    }

    public void setTitleText(String title) {
        XposedHelpers.callMethod(mNavigationBar, "setTitleText", title);
    }
}
