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

    var bdSwitch: View
    private var mMethods: Array<Method>

    init {
        val cls = findClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView")
        bdSwitch = XposedHelpers.newInstance(cls, getContext()) as View
        mMethods = cls.declaredMethods
    }

    fun setOnSwitchStateChangeListener(l: InvocationHandler) {
        val clazz: Class<*> = try {
            findClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView\$b")
        } catch (e: ClassNotFoundError) {
            findClass("com.baidu.adp.widget.BdSwitchView.BdSwitchView\$a")
        }
        val proxy = Proxy.newProxyInstance(sClassLoader, arrayOf(clazz), l)
        XposedHelpers.callMethod(bdSwitch, "setOnSwitchStateChangeListener", proxy)
    }

    fun isOn(): Boolean = try {
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

    fun getVibrator(): Vibrator? = getObjectField(bdSwitch, Vibrator::class.java)
}
