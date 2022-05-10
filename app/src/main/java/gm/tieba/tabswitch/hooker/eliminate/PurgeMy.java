package gm.tieba.tabswitch.hooker.eliminate;

import android.view.View;
import android.widget.ImageView;

import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.ReflectUtils;

public class PurgeMy extends XposedContext implements IHooker {
    public void hook() throws Throwable {
        AcRules.findRule(Constants.getMatchers().get(PurgeMy.class), (AcRules.Callback) (matcher, clazz, method) -> {
            switch (matcher) {
                case "Lcom/baidu/tieba/R$drawable;->icon_pure_topbar_store44_svg:I": // 商店
                    XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // R.id.person_navigation_dressup_img
                            var imageView = (ImageView) ReflectUtils.getObjectField(param.thisObject, 4);
                            imageView.setVisibility(View.GONE);
                        }
                    });
                    break;
                case "Lcom/baidu/tieba/R$drawable;->person_center_red_tip_shape:I": // 分割线
                    if ("com.baidu.tieba.post.PersonPostActivity".equals(clazz)) {
                        break;
                    }
                    for (var md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                        if (Arrays.toString(md.getParameterTypes()).equals("[interface com.baidu.tbadk.TbPageContext, int]")) {
                            XposedBridge.hookMethod(md, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    // R.id.function_item_bottom_divider
                                    var view = (View) ReflectUtils.getObjectField(param.thisObject, 10);
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
