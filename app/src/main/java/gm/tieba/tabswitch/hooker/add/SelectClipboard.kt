package gm.tieba.tabswitch.hooker.add

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.TextView
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher
import gm.tieba.tabswitch.util.fixAlertDialogWidth
import gm.tieba.tabswitch.util.getCurrentActivity
import gm.tieba.tabswitch.util.getDialogTheme
import gm.tieba.tabswitch.util.getObjectField
import gm.tieba.tabswitch.util.getTbadkCoreApplicationInst
import gm.tieba.tabswitch.util.isLightMode
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.query.matchers.MethodsMatcher

class SelectClipboard : XposedContext(), IHooker, Obfuscated {
    override fun key(): String {
        return "select_clipboard"
    }

    override fun matchers(): List<Matcher> {
        return listOf(
            SmaliMatcher("Landroid/text/ClipboardManager;->setText(Ljava/lang/CharSequence;)V").apply {
                classMatcher = ClassMatcher.create().methods(
                    MethodsMatcher.create().add(
                        MethodMatcher.create().addInvoke(
                            MethodMatcher.create()
                                .descriptor("Lcom/baidu/tbadk/core/data/SmallTailInfo;-><init>()V")
                        )
                    )
                )
            }
        )
    }

    @Throws(Throwable::class)
    override fun hook() {
        findRule(matchers()) { matcher, clazz, method ->
            when (matcher) {
                "Landroid/text/ClipboardManager;->setText(Ljava/lang/CharSequence;)V" ->
                    hookReplaceMethod(clazz, method) { param ->
                        val tbRichText = getObjectField(
                            param.thisObject,
                            "com.baidu.tbadk.widget.richText.TbRichText"
                        )
                        val currentActivity = getCurrentActivity()
                        AlertDialog.Builder(
                            currentActivity,
                            getDialogTheme(getContext())
                        )
                            .setTitle("自由复制")
                            .setMessage(tbRichText.toString())
                            .setNeutralButton("复制全部") { _, _ ->
                                val clipboardManager =
                                    getTbadkCoreApplicationInst().getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                    ) as ClipboardManager
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("tieba", tbRichText.toString()))
                            }
                            .setPositiveButton("完成", null)
                            .create()
                            .apply {
                                show()
                                findViewById<TextView>(android.R.id.message)?.setTextIsSelectable(true)
                                fixAlertDialogWidth(this)
                            }
                        null
                    }
            }
        }
    }
}
