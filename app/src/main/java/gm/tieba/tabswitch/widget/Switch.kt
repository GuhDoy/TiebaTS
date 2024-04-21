package gm.tieba.tabswitch.widget

import android.os.Vibrator
import android.view.View
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.util.callMethod
import gm.tieba.tabswitch.util.getObjectField
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class Switch : XposedContext() {
    @JvmField
    var bdSwitch: View
    private var mMethods: Array<Method>

    init {
        val cls =
            XposedHelpers.findClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView", sClassLoader)
        bdSwitch = XposedHelpers.newInstance(cls, context) as View
        mMethods = cls.declaredMethods
    }

    fun setOnSwitchStateChangeListener(l: InvocationHandler) {
        val clazz: Class<*> = try {
            XposedHelpers.findClass(
                "com.baidu.adp.widget.BdSwitchView.BdSwitchView\$b",
                sClassLoader
            )
        } catch (e: ClassNotFoundError) {
            XposedHelpers.findClass(
                "com.baidu.adp.widget.BdSwitchView.BdSwitchView\$a",
                sClassLoader
            )
        }
        val proxy = Proxy.newProxyInstance(sClassLoader, arrayOf(clazz), l)
        XposedHelpers.callMethod(bdSwitch, "setOnSwitchStateChangeListener", proxy)
    }

    val isOn: Boolean
        get() = try {
            XposedHelpers.callMethod(bdSwitch, "isOn") as Boolean
        } catch (e: NoSuchMethodError) {
            callMethod(mMethods[6], bdSwitch) as Boolean
        }

    fun changeState() {
        try {
            XposedHelpers.callMethod(bdSwitch, "changeState")
        } catch (e: NoSuchMethodError) {
            callMethod(mMethods[3], bdSwitch)
        }
    }

    fun turnOn() {
        try {
            XposedHelpers.callMethod(bdSwitch, "turnOn")
        } catch (e: NoSuchMethodError) {
            callMethod(mMethods[11], bdSwitch)
        }
    }

    fun turnOff() {
        try {
            XposedHelpers.callMethod(bdSwitch, "turnOff")
        } catch (e: NoSuchMethodError) {
            callMethod(mMethods[8], bdSwitch)
        }
    }

    val vibrator: Vibrator?
        get() = getObjectField(bdSwitch, Vibrator::class.java)
}
