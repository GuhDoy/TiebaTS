package gm.tieba.tabswitch.hooker.add;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.Reflect;

public class Ripple extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        // 楼中楼
        AcRules.findRule(sRes.getString(R.string.Ripple), new AcRules.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View newSubPbListItem = (View) param.getResult();
                        View view = newSubPbListItem.findViewById(
                                Reflect.getId("new_sub_pb_list_richText"));
                        view.setBackground(createSubPbBackground());
                    }
                });
            }
        });
        // 查看全部回复
        XposedHelpers.findAndHookConstructor("com.baidu.tieba.pb.pb.sub.SubPbLayout", sClassLoader,
                Context.class, AttributeSet.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.get(param.thisObject) instanceof RelativeLayout) {
                                View view = (View) field.get(param.thisObject);
                                view.setBackground(createSubPbBackground());
                                return;
                            }
                        }
                    }
                });
        // 楼层
        XposedHelpers.findAndHookConstructor("com.baidu.tieba.pb.pb.main.PbCommenFloorItemViewHolder", sClassLoader,
                XposedHelpers.findClass("com.baidu.tbadk.TbPageContext", sClassLoader), View.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (field.get(param.thisObject) instanceof LinearLayout) {
                                View view = (View) field.get(param.thisObject);
                                if (view.getId() == Reflect.getId("all_content")) {
                                    view.setBackground(createBackground());
                                    return;
                                }
                            }
                        }
                    }
                });
    }

    private StateListDrawable createBackground() throws Throwable {
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed},
                new ColorDrawable(Reflect.getColor("CAM_X0204")));
        return sld;
    }

    private StateListDrawable createSubPbBackground() throws Throwable {
        if (!DisplayHelper.getTbSkin(getContext()).equals("")) {
            return createBackground();
        } else {
            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.WHITE));
            return sld;
        }
    }
}
