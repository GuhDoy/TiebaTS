package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.MatcherProperties;
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher;
import gm.tieba.tabswitch.util.ClassMatcherUtils;
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
                new SmaliMatcher("Landroid/text/ClipboardManager;->setText(Ljava/lang/CharSequence;)V",
                        MatcherProperties.create().useClassMatcher(ClassMatcherUtils.invokeMethod("Lcom/baidu/tieba/tbadkCore/data/ThemeBubbleData;-><init>(Ltbclient/ThemeBubble;)V")))
        );
    }

    public void hook() throws Throwable {
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            switch (matcher) {
                case "Lcom/baidu/tieba/tbadkCore/data/ThemeBubbleData;-><init>(Ltbclient/ThemeBubble;)V/Landroid/text/ClipboardManager;->setText(Ljava/lang/CharSequence;)V":
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

                            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                            layoutParams.copyFrom(alert.getWindow().getAttributes());
                            layoutParams.width = DisplayUtils.getDisplayWidth(getContext());
                            alert.getWindow().setAttributes(layoutParams);

                            return null;
                        }
                    });
                    break;
            }
        });
    }
}
