package gm.tieba.tabswitch.widget

import android.view.View
import android.widget.TextView
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.util.getColor
import gm.tieba.tabswitch.util.getObjectField

class NavigationBar(thisObject: Any) : XposedContext() {

    private val mNavigationBar: Any? = getObjectField(
        thisObject,
        "com.baidu.tbadk.core.view.NavigationBar"
    )

    fun addTextButton(text: String?, l: View.OnClickListener?) {
        val controlAlignClass = findClass("com.baidu.tbadk.core.view.NavigationBar\$ControlAlign")
        val horizontalRight = controlAlignClass.enumConstants.find { it.toString() == "HORIZONTAL_RIGHT" }
            ?: throw IllegalStateException("HORIZONTAL_RIGHT enum constant not found")
        val textView = XposedHelpers.callMethod(
            mNavigationBar,
            "addTextButton", horizontalRight, text, l
        ) as TextView
        textView.setTextColor(getColor("CAM_X0105"))
    }

    fun setTitleText(title: String?) {
        title?.let {
            XposedHelpers.callMethod(mNavigationBar, "setTitleText", it)
        }
    }

    fun setCenterTextTitle(title: String?) {
        title?.let {
            XposedHelpers.callMethod(mNavigationBar, "setCenterTextTitle", it)
        }
    }
}
