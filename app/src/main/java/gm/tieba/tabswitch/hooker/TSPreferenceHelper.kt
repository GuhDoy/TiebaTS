package gm.tieba.tabswitch.hooker

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.Constants.strings
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.Preferences.getBoolean
import gm.tieba.tabswitch.dao.Preferences.getString
import gm.tieba.tabswitch.dao.Preferences.putBoolean
import gm.tieba.tabswitch.dao.Preferences.putString
import gm.tieba.tabswitch.dao.Preferences.remove
import gm.tieba.tabswitch.util.dipToPx
import gm.tieba.tabswitch.util.fixAlertDialogWidth
import gm.tieba.tabswitch.util.getColor
import gm.tieba.tabswitch.util.getCurrentActivity
import gm.tieba.tabswitch.util.getDialogTheme
import gm.tieba.tabswitch.util.getDimen
import gm.tieba.tabswitch.util.getDimenDip
import gm.tieba.tabswitch.util.getDrawableId
import gm.tieba.tabswitch.util.getObjectField
import gm.tieba.tabswitch.util.getR
import gm.tieba.tabswitch.util.isLightMode
import gm.tieba.tabswitch.widget.Switch
import gm.tieba.tabswitch.widget.TbToast
import gm.tieba.tabswitch.widget.TbToast.Companion.showTbToast
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.Random
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

object TSPreferenceHelper : XposedContext() {
    @JvmStatic
    fun createTextView(text: String?): TextView = TextView(getContext()).apply {
        this.text = text
        setTextColor(getColor("CAM_X0108"))
        textSize = getDimenDip("fontsize22")

        layoutParams = text?.let {
            setPaddingRelative(
                getDimen("ds30").toInt(),
                getDimen("ds20").toInt(),
                0,
                getDimen("ds20").toInt()
            )
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        } ?: LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getDimen("ds32").toInt())
    }

    @JvmStatic
    @SuppressLint("ClickableViewAccessibility")
    fun createButton(text: String?, tip: String?, showArrow: Boolean, l: View.OnClickListener?): LinearLayout {
        val textTipView = XposedHelpers.newInstance(
            findClass("com.baidu.tbadk.coreExtra.view.TbSettingTextTipView"),
            getContext()
        )
        XposedHelpers.callMethod(textTipView, "setText", text)
        XposedHelpers.callMethod(textTipView, "setTip", tip)

        val imageView = getObjectField(textTipView, ImageView::class.java)?.apply {
            visibility = if (showArrow) View.VISIBLE else View.GONE
        }

        val svgManager = XposedHelpers.callStaticMethod(
            findClass("com.baidu.tbadk.core.util.SvgManager"),
            "getInstance"
        )

        XposedHelpers.callMethod(
            svgManager,
            "setPureDrawableWithDayNightModeAutoChange",
            imageView,
            getDrawableId("icon_pure_list_arrow16_right_svg"),
            getR("color", "CAM_X0109"),
            null
        )

        val newButton = getObjectField(textTipView, LinearLayout::class.java)
        newButton?.apply {
            (parent as ViewGroup).removeView(this)
            l?.let { setOnClickListener(it) }

            val backgroundColor = getColor("CAM_X0201")
            setBackgroundColor(backgroundColor)

            if (showArrow) {
                setOnTouchListener { v, event ->
                    val isInside = event.x in 0f..v.width.toFloat() && event.y in 0f..v.height.toFloat()

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> setBackgroundColor(
                            Color.argb(
                                128,
                                Color.red(backgroundColor),
                                Color.green(backgroundColor),
                                Color.blue(backgroundColor)
                            )
                        )
                        MotionEvent.ACTION_MOVE -> if (!isInside) setBackgroundColor(backgroundColor)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> setBackgroundColor(backgroundColor)
                    }
                    false
                }
            }

            // Fix TS Preference button not changing background when skin type changed
            hookAfterMethod(
                "com.baidu.tieba.setting.more.MoreActivity",
                "onChangeSkinType", Int::class.javaPrimitiveType
            ) { _ ->
                setBackgroundColor(getColor("CAM_X0201"))
                XposedHelpers.callMethod(
                    svgManager,
                    "setPureDrawableWithDayNightModeAutoChange",
                    imageView,
                    getDrawableId("icon_pure_list_arrow16_right_svg"),
                    getR("color", "CAM_X0109"),
                    null
                )
            }
        }

        return newButton ?: throw IllegalStateException("LinearLayout not found in TbSettingTextTipView")
    }

    @JvmStatic
    fun randomToast(): String {
        return when (Random().nextInt(5)) {
            0 -> "别点了，新版本在做了"
            1 -> "别点了别点了T_T"
            2 -> "再点人傻了>_<"
            3 -> "点了也没用~"
            4 -> "点个小星星吧:)"
            else -> ""
        }
    }

    class PreferenceLayout(context: Context?) : LinearLayout(context) {
        init {
            orientation = VERTICAL
        }

        fun addView(view: SwitchButtonHolder) {
            addView(view.switchButton)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    class SwitchButtonHolder(activity: Activity, text: String?, private val mKey: String, type: Int) {
        lateinit var bdSwitch: Switch
        lateinit var switchButton: LinearLayout

        init {
            if (exceptions.containsKey(mKey)) {
                switchButton = createButton(text, "此功能初始化失败", false) { _ ->
                    val tr = exceptions[mKey]
                    XposedBridge.log(tr)
                    showTbToast(Log.getStackTraceString(tr), TbToast.LENGTH_SHORT)
                }

            } else {
                bdSwitch = Switch().apply {
                    setOnSwitchStateChangeListener(SwitchStatusChangeHandler())
                }

                val bdSwitchView = bdSwitch.bdSwitch.apply {
                    layoutParams = LinearLayout.LayoutParams(width, height, 0.16f)
                    id = View.generateViewId()
                }

                when (type) {
                    TYPE_SWITCH -> {
                        switchButton = createButton(text, null, false) { _ -> bdSwitch.changeState() }
                        sIdToTag[bdSwitchView.id] = "$TYPE_SWITCH$mKey"
                        if (getBoolean(mKey)) bdSwitch.turnOn() else bdSwitch.turnOff()
                    }

                    TYPE_DIALOG -> {
                        switchButton = createButton(text, null, false) { _ -> showRegexDialog(activity, text) }
                        bdSwitchView.setOnTouchListener { _, _ -> false }
                        if (getString(mKey) != null) bdSwitch.turnOn() else bdSwitch.turnOff()
                    }
                }
                switchButton.addView(bdSwitchView)
            }
        }

        fun setOnButtonClickListener(l: View.OnClickListener?) {
            switchButton.setOnClickListener(l)
            bdSwitch.bdSwitch.setOnTouchListener { _, _ ->
                XposedHelpers.callMethod(bdSwitch.getVibrator(), "vibrate", 30L)
                false
            }
        }

        private fun showRegexDialog(activity: Activity, title: String?) {
            val currentActivity = getCurrentActivity()
            val isLightMode = isLightMode(getContext())

            val editText = EditText(currentActivity).apply {
                hint = strings["regex_hint"]
                setText(getString(mKey))
                if (!isLightMode) {
                    setTextColor(Color.WHITE)
                    setHintTextColor(Color.GRAY)
                }
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                isFallbackLineSpacing = false
                setLineSpacing(0f, 1.2f)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    leftMargin = dipToPx(currentActivity, 20f)
                    rightMargin = dipToPx(currentActivity, 20f)
                }
            }

            val linearLayout = LinearLayout(currentActivity).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(editText)
            }

            val alert = AlertDialog.Builder(
                currentActivity,
                getDialogTheme(getContext())
            )
                .setTitle(title)
                .setView(linearLayout)
                .setNegativeButton(activity.getString(android.R.string.cancel), null)
                .setPositiveButton(activity.getString(android.R.string.ok), null)
                .create()

            alert.setOnShowListener { _ ->
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { _ ->
                    try {
                        if (TextUtils.isEmpty(editText.text)) {
                            remove(mKey)
                            bdSwitch.turnOff()
                        } else {
                            Pattern.compile(editText.text.toString())
                            putString(mKey, editText.text.toString())
                            bdSwitch.turnOn()
                        }
                        alert.dismiss()
                    } catch (e: PatternSyntaxException) {
                        showTbToast(e.message, TbToast.LENGTH_SHORT)
                    }
                }
            }

            alert.show()
            fixAlertDialogWidth(alert)
            alert.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

            editText.imeOptions = EditorInfo.IME_ACTION_DONE
            editText.setOnEditorActionListener { _, actionId: Int, event: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE || event != null
                    && event.keyCode == KeyEvent.KEYCODE_ENTER
                ) {
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                    true
                } else {
                    false
                }
            }
            editText.requestFocus()
        }

        private class SwitchStatusChangeHandler : InvocationHandler {
            override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any? {
                val view = args[0] as View
                val tag = sIdToTag[view.id]
                tag?.let {
                    if (it.substring(0, 1).toInt() == TYPE_SWITCH) {
                        putBoolean(it.substring(1), args[1].toString() == "ON")
                    }
                }
                return null
            }
        }

        companion object {
            const val TYPE_SWITCH: Int = 0
            const val TYPE_DIALOG: Int = 1
            val sIdToTag: MutableMap<Int, String> = HashMap()
        }
    }
}
