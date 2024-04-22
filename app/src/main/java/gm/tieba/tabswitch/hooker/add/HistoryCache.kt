package gm.tieba.tabswitch.hooker.add

import android.R
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.Constants.strings
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.util.dipToPx
import gm.tieba.tabswitch.util.findFirstMethodByExactType
import gm.tieba.tabswitch.util.fixAlertDialogWidth
import gm.tieba.tabswitch.util.getCurrentActivity
import gm.tieba.tabswitch.util.getObjectField
import gm.tieba.tabswitch.util.isLightMode
import gm.tieba.tabswitch.widget.NavigationBar
import gm.tieba.tabswitch.widget.TbToast
import gm.tieba.tabswitch.widget.TbToast.Companion.showTbToast
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class HistoryCache : XposedContext(), IHooker {
    private var mRegex = ""
    override fun key(): String {
        return "history_cache"
    }

    @Throws(Throwable::class)
    override fun hook() {
        hookAfterMethod(
            "com.baidu.tieba.myCollection.history.PbHistoryActivity",
            "onCreate", Bundle::class.java
        ) {param ->
            val activity = param.thisObject as Activity
            if (param.args[0] == null) {
                mRegex = ""
            }
            NavigationBar(param.thisObject)
                .addTextButton("搜索") { showRegexDialog(activity) }
        }

        hookBeforeMethod(
            findFirstMethodByExactType("com.baidu.tieba.myCollection.history.PbHistoryActivity", MutableList::class.java)
        ) { param ->
            val list = param.args[0] as MutableList<*>
            val pattern = Pattern.compile(mRegex, Pattern.CASE_INSENSITIVE)

            list.removeIf { obj: Any? ->
                val strings = try {
                    arrayOf(
                        XposedHelpers.getObjectField(obj, "forumName") as String,
                        XposedHelpers.getObjectField(obj, "threadName") as String
                    )
                } catch (e: NoSuchFieldError) {
                    arrayOf(
                        getObjectField(obj, 3) as String,
                        getObjectField(obj, 2) as String
                    )
                }
                strings.none { string -> pattern.matcher(string).find() }
            }
        }
    }

    private fun showRegexDialog(activity: Activity) {
        val currentActivity = getCurrentActivity()
        val isLightMode = isLightMode(getContext())

        val editText = EditText(currentActivity).apply {
            setHint(strings["regex_hint"])
            setText(mRegex)
            if (!isLightMode) {
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
            }
            setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
            setFallbackLineSpacing(false)
            setLineSpacing(0f, 1.2f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    mRegex = s.toString()
                }
            })
        }

        val linearLayout = LinearLayout(currentActivity).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dipToPx(currentActivity, 20f)
            rightMargin = dipToPx(currentActivity, 20f)
        }
        editText.setLayoutParams(layoutParams)
        linearLayout.addView(editText)

        val currRegex = mRegex
        val alert = AlertDialog.Builder(
            currentActivity,
            if (isLightMode) R.style.Theme_DeviceDefault_Light_Dialog_Alert else R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("搜索")
            .setView(linearLayout)
            .setOnCancelListener { mRegex = currRegex }
            .setNegativeButton(activity.getString(R.string.cancel)) { _, _ -> mRegex = currRegex }
            .setPositiveButton(activity.getString(R.string.ok), null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        try {
                            Pattern.compile(editText.getText().toString())
                            dismiss()
                            activity.recreate()
                        } catch (e: PatternSyntaxException) {
                            showTbToast(e.message, TbToast.LENGTH_SHORT)
                        }
                    }
                }
                show()
                fixAlertDialogWidth(this)
            }

        alert.window?.apply {setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)}

        editText.apply {
            setSingleLine()
            setImeOptions(EditorInfo.IME_ACTION_SEARCH)
            setOnEditorActionListener { _: TextView?, actionId: Int, event: KeyEvent? ->
                when {
                    actionId == EditorInfo.IME_ACTION_SEARCH || event?.keyCode == KeyEvent.KEYCODE_ENTER -> {
                        alert.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                        true
                    }
                    else -> false
                }
            }
            selectAll()
            requestFocus()
        }
    }
}
