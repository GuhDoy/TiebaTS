package gm.tieba.tabswitch.widget;

import android.view.View;
import android.widget.TextView;

import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.util.ReflectUtils;

public class NavigationBar extends XposedContext {
    private final Object mNavigationBar;

    public NavigationBar(final Object thisObject) {
        mNavigationBar = ReflectUtils.getObjectField(thisObject,
                "com.baidu.tbadk.core.view.NavigationBar");
    }

    public void addTextButton(final String text, final View.OnClickListener l) {
        final Class<?> ControlAlign = XposedHelpers.findClass(
                "com.baidu.tbadk.core.view.NavigationBar$ControlAlign", sClassLoader);
        for (final Object HORIZONTAL_RIGHT : ControlAlign.getEnumConstants()) {
            if (HORIZONTAL_RIGHT.toString().equals("HORIZONTAL_RIGHT")) {
                final TextView textView = (TextView) XposedHelpers.callMethod(mNavigationBar,
                        "addTextButton", HORIZONTAL_RIGHT, text, l);
                textView.setTextColor(ReflectUtils.getColor("CAM_X0105"));
                break;
            }
        }
    }

    public void setTitleText(final String title) {
        XposedHelpers.callMethod(mNavigationBar, "setTitleText", title);
    }
}
