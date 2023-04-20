package gm.tieba.tabswitch.widget;

import android.view.View;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.util.ReflectUtils;

public class Switch extends XposedContext {
    public View bdSwitch;
    private Method[] mMethods;

    public Switch() {
        final var cls = XposedHelpers.findClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView", sClassLoader);
        bdSwitch = (View) XposedHelpers.newInstance(cls, getContext());
        mMethods = cls.getDeclaredMethods();
    }

    public Switch(final View bdSwitch) {
        this.bdSwitch = bdSwitch;
    }

    public void setOnSwitchStateChangeListener(final InvocationHandler l) {
        Class<?> clazz;
        try {
            clazz = XposedHelpers.findClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView$b", sClassLoader);
        } catch (final XposedHelpers.ClassNotFoundError e) {
            clazz = XposedHelpers.findClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView$a", sClassLoader);
        }
        final Object proxy = Proxy.newProxyInstance(sClassLoader, new Class<?>[]{clazz}, l);
        XposedHelpers.callMethod(bdSwitch, "setOnSwitchStateChangeListener", proxy);
    }

    public boolean isOn() {
        try {
            return (Boolean) XposedHelpers.callMethod(bdSwitch, "isOn");
        } catch (final NoSuchMethodError e) {
            return (Boolean) ReflectUtils.callMethod(mMethods[6], bdSwitch);
        }
    }

    public void changeState() {
        try {
            XposedHelpers.callMethod(bdSwitch, "changeState");
        } catch (final NoSuchMethodError e) {
            ReflectUtils.callMethod(mMethods[3], bdSwitch);
        }
    }

    public void turnOn() {
        try {
            XposedHelpers.callMethod(bdSwitch, "turnOn");
        } catch (final NoSuchMethodError e) {
            ReflectUtils.callMethod(mMethods[11], bdSwitch);
        }
    }

    public void turnOff() {
        try {
            XposedHelpers.callMethod(bdSwitch, "turnOff");
        } catch (final NoSuchMethodError e) {
            ReflectUtils.callMethod(mMethods[8], bdSwitch);
        }
    }
}
