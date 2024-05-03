package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.MethodsMatcher;

import java.util.List;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.util.ReflectUtils;

public class SelectClipboard extends XposedContext implements IHooker, Obfuscated {
    @NonNull
    @Override
    public String key() {
        return "select_clipboard";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(
                new SmaliMatcher("Landroid/text/ClipboardManager;->setText(Ljava/lang/CharSequence;)V")
                        .setBaseClassMatcher(ClassMatcher.create().methods(
                                MethodsMatcher.create().add(MethodMatcher.create().addInvoke(
                                        MethodMatcher.create().descriptor("Lcom/baidu/tbadk/core/data/SmallTailInfo;-><init>()V")
                                )))
                        )
        );
    }

    public void hook() throws Throwable {
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            switch (matcher) {
                case "Landroid/text/ClipboardManager;->setText(Ljava/lang/CharSequence;)V":
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            Object tbRichText = ReflectUtils.getObjectField(param.thisObject, "com.baidu.tbadk.widget.richText.TbRichText");

                            Activity currentActivity = ReflectUtils.getCurrentActivity();
                            AlertDialog alert = new AlertDialog.Builder(currentActivity, DisplayUtils.isLightMode(getContext()) ?
                                    android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                    .setTitle("自由复制").setMessage(tbRichText.toString())
                                    .setNeutralButton("复制全部", (dialogInterface, i) -> {
                                        ClipboardManager clipboardManager = (ClipboardManager) ReflectUtils.getTbadkCoreApplicationInst().getSystemService(Context.CLIPBOARD_SERVICE);
                                        clipboardManager.setText(tbRichText.toString());
                                    })
                                    .setPositiveButton("完成", null).create();
                            alert.show();

                            View messageView = alert.findViewById(android.R.id.message);
                            if (messageView instanceof TextView) {
                                ((TextView) messageView).setTextIsSelectable(true);
                            }

                            DisplayUtils.fixAlertDialogWidth(alert);
                            return null;
                        }
                    });
                    break;
            }
        });
    }
}
