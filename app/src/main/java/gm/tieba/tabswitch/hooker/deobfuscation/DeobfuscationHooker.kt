package gm.tieba.tabswitch.hooker.deobfuscation

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules.dropAllRules
import gm.tieba.tabswitch.dao.Preferences.getBoolean
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.getTbVersion
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.isDexChanged
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper.saveAndRestart
import kotlin.concurrent.thread
import kotlin.properties.Delegates

class DeobfuscationHooker(private val mMatchers: List<Matcher>) : XposedContext(), IHooker {

    var progress : Float by Delegates.observable(0f) { _, _, new ->
        updateProgress(new)
    }

    private val deobfuscation = Deobfuscation()
    private lateinit var mActivity: Activity
    private lateinit var mProgress: View
    private lateinit var mMessage: TextView
    private lateinit var mProgressContainer: FrameLayout
    private lateinit var mContentView: LinearLayout

    override fun key(): String {
        return "deobfs"
    }

    @SuppressLint("ApplySharedPref", "CheckResult")
    override fun hook() {
        hookAfterMethod(
            "com.baidu.tieba.LogoActivity",
            "onCreate", Bundle::class.java
        ) { param ->
            val hooks = disableStartAndFinishActivity()
            mActivity = param.thisObject as Activity
            if (getBoolean("purge")) {
                mActivity.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .edit()
                    .putString("key_location_request_dialog_last_show_version", getTbVersion(mActivity))
                    .commit()
            }

            if (isDexChanged(mActivity)) {
                dropAllRules()
            } else {
                hooks.forEach { it.unhook() }
                saveAndRestart(mActivity, getTbVersion(mActivity), findClass(TRAMPOLINE_ACTIVITY))
                return@hookAfterMethod
            }

            initProgressIndicator()
            mActivity.addContentView(
                mContentView, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
                )
            )

            thread {
                try {
                    setMessage("搜索资源，字符串和方法调用")
                    performDeobfuscation(mActivity, mMatchers)

                    XposedBridge.log("Deobfuscation complete, current version: ${getTbVersion(mActivity)}")
                    hooks.forEach { it.unhook() }
                    saveAndRestart(
                        mActivity,
                        getTbVersion(mActivity),
                        findClass(TRAMPOLINE_ACTIVITY)
                    )
                } catch (e: Throwable) {
                    XposedBridge.log(e)
                    setMessage("处理失败\n${Log.getStackTraceString(e)}")
                }
            }
        }
    }

    private fun performDeobfuscation(context: Context, matchers: List<Matcher>) {
        deobfuscation.setMatchers(matchers)
        deobfuscation.dexkit(context, this)
        deobfuscation.saveDexSignatureHashCode()
    }

    private fun disableStartAndFinishActivity(): List<XC_MethodHook.Unhook> {
        return listOf(
            hookReplaceMethod(Instrumentation::class.java, "execStartActivity",
                Context::class.java, IBinder::class.java, IBinder::class.java, Activity::class.java, Intent::class.java,
                Int::class.javaPrimitiveType, Bundle::class.java) { null },
            hookReplaceMethod(Activity::class.java, "finish",
                Int::class.javaPrimitiveType) { null },
            hookReplaceMethod(Activity::class.java, "finishActivity",
                Int::class.javaPrimitiveType) { null },
            hookReplaceMethod(Activity::class.java, "finishAffinity") { null }
        )
    }

    @SuppressLint("SetTextI18n")
    private fun initProgressIndicator() {
        val title = TextView(mActivity).apply {
            textSize = 16f
            setPaddingRelative(0, 0, 0, 8)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.parseColor("#FF303030"))
            text = "贴吧TS正在定位被混淆的类和方法，请耐心等待"
        }

        mProgress = View(mActivity).apply {
            setBackgroundColor(Color.parseColor("#FFBEBEBE"))
        }

        mMessage = TextView(mActivity).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            textSize = 16f
            setTextColor(Color.parseColor("#FF303030"))
            layoutParams = FrameLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        mProgressContainer = FrameLayout(mActivity).apply {
            addView(mProgress)
            addView(mMessage)
            layoutParams = FrameLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val progressIndicator = LinearLayout(mActivity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(title)
            addView(mProgressContainer)
            setPaddingRelative(0, 16, 0, 16)
            layoutParams =  LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        mContentView = LinearLayout(mActivity).apply {
            gravity = Gravity.CENTER
            addView(progressIndicator)
        }
    }

    private fun setMessage(message: String) {
        mActivity.runOnUiThread { mMessage.text = message }
    }

    private fun updateProgress(progress: Float) {
        mActivity.runOnUiThread {
            mProgress.layoutParams = mProgress.layoutParams.apply {
                height = mMessage.height
                width = Math.round(mProgressContainer.width * progress)
            }
        }
    }

    companion object {
        private const val TRAMPOLINE_ACTIVITY = "com.baidu.tieba.tblauncher.MainTabActivity"
    }
}
