package gm.tieba.tabswitch.widget;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.XposedBridge;

public class Switch {
    private final ClassLoader mClassLoader;
    private Class<?> mClass;
    public View bdSwitch;

    public Switch(ClassLoader classLoader, Activity activity) {
        mClassLoader = classLoader;
        try {
            mClass = classLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView");
            bdSwitch = (View) mClass.getConstructor(Context.class).newInstance(activity);
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    public Switch(ClassLoader classLoader, View bdSwitch) {
        mClassLoader = classLoader;
        try {
            mClass = classLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView");
            this.bdSwitch = bdSwitch;
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    public void setOnSwitchStateChangeListener(InvocationHandler onSwitchStateChange) {
        try {
            Class<?> clazz;
            try {
                clazz = mClassLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView$b");
            } catch (ClassNotFoundException e) {
                clazz = mClassLoader.loadClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView$a");
            }
            Object proxy = Proxy.newProxyInstance(mClassLoader, new Class<?>[]{clazz}, onSwitchStateChange);
            mClass.getDeclaredMethod("setOnSwitchStateChangeListener",
                    clazz).invoke(bdSwitch, proxy);
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    public boolean isOn() {
        try {
            try {
                return (Boolean) mClass.getDeclaredMethod("isOn").invoke(bdSwitch);
            } catch (NoSuchMethodException e) {
                return (Boolean) mClass.getDeclaredMethod("d").invoke(bdSwitch);
            }
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
        return false;
    }

    public void changeState() {
        try {
            Method changeState;
            try {
                changeState = mClass.getDeclaredMethod("changeState");
            } catch (NoSuchMethodException e) {
                changeState = mClass.getDeclaredMethod("b");
            }
            changeState.setAccessible(true);
            changeState.invoke(bdSwitch);
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    public void turnOn() {
        try {
            try {
                mClass.getDeclaredMethod("turnOn").invoke(bdSwitch);
            } catch (NoSuchMethodException e) {
                mClass.getDeclaredMethod("i").invoke(bdSwitch);
            }
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    public void turnOff() {
        try {
            try {
                mClass.getDeclaredMethod("turnOff").invoke(bdSwitch);
            } catch (NoSuchMethodException e) {
                mClass.getDeclaredMethod("f").invoke(bdSwitch);
            }
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }
}
