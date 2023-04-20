package gm.tieba.tabswitch.hooker.eliminate;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.Obfuscated;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.util.ReflectUtils;

@Deprecated
public class PurgeMy extends XposedContext implements IHooker, Obfuscated {

    @NonNull
    @Override
    public String key() {
        return "purge_my";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return null;
    }

    @Override
    public void hook() throws Throwable {
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            switch (matcher) {
                case "Lcom/baidu/tieba/R$drawable;->icon_pure_topbar_store44_svg:I": // 商店
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            // R.id.person_navigation_dressup_img
                            final var imageView = (ImageView) ReflectUtils.getObjectField(param.thisObject, 4);
                            imageView.setVisibility(View.GONE);
                        }
                    });
                    break;
                case "Lcom/baidu/tieba/R$drawable;->person_center_red_tip_shape:I": // 分割线
                    if ("com.baidu.tieba.post.PersonPostActivity".equals(clazz)) {
                        break;
                    }
                    for (final var md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                        if (Arrays.toString(md.getParameterTypes()).equals("[interface com.baidu.tbadk.TbPageContext, int]")) {
                            XposedBridge.hookMethod(md, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                                    // R.id.function_item_bottom_divider
                                    final var view = (View) ReflectUtils.getObjectField(param.thisObject, 10);
                                    view.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                    break;
            }
        });
    }
}
