package gm.tieba.tabswitch.widget;

import android.view.View;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;

public class Switch extends XposedContext {
    public View bdSwitch;

    public Switch() {
        bdSwitch = (View) XposedHelpers.newInstance(XposedHelpers.findClass(
                "com.baidu.adp.widget.BdSwitchView.BdSwitchView", sClassLoader), getContext());
    }

    public Switch(View bdSwitch) {
        this.bdSwitch = bdSwitch;
    }

    public void setOnSwitchStateChangeListener(InvocationHandler l) {
        Class<?> clazz;
        try {
            clazz = XposedHelpers.findClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView$b", sClassLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            clazz = XposedHelpers.findClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView$a", sClassLoader);
        }
        Object proxy = Proxy.newProxyInstance(sClassLoader, new Class<?>[]{clazz}, l);
        XposedHelpers.callMethod(bdSwitch, "setOnSwitchStateChangeListener", proxy);
    }

    public boolean isOn() {
        try {
            return (Boolean) XposedHelpers.callMethod(bdSwitch, "isOn");
        } catch (NoSuchMethodError e) {
            return (Boolean) XposedHelpers.callMethod(bdSwitch, "d");
        }
    }

    public void changeState() {
        try {
            XposedHelpers.callMethod(bdSwitch, "changeState");
        } catch (NoSuchMethodError e) {
            XposedHelpers.callMethod(bdSwitch, "b");
        }
    }

    public void turnOn() {
        try {
            XposedHelpers.callMethod(bdSwitch, "turnOn");
        } catch (NoSuchMethodError e) {
            XposedHelpers.callMethod(bdSwitch, "j");
        }
    }

    public void turnOff() {
        try {
            XposedHelpers.callMethod(bdSwitch, "turnOff");
        } catch (NoSuchMethodError e) {
            XposedHelpers.callMethod(bdSwitch, "f");
        }
    }
}
