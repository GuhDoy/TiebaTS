package gm.tieba.tabswitch.hooker.add;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.util.ReflectUtils;

public class Ripple extends XposedContext implements IHooker {
    public void hook() throws Throwable {
        // 楼中楼
        AcRules.findRule(Constants.getMatchers().get(Ripple.class), (AcRules.Callback) (rule, clazz, method) ->
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View newSubPbListItem = (View) param.getResult();
                        View view = newSubPbListItem.findViewById(
                                ReflectUtils.getId("new_sub_pb_list_richText"));
                        view.setBackground(createSubPbBackground());
                    }
                }));
        // 查看全部回复
        XposedHelpers.findAndHookConstructor("com.baidu.tieba.pb.pb.sub.SubPbLayout", sClassLoader,
                Context.class, AttributeSet.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) ReflectUtils.getObjectField(param.thisObject, RelativeLayout.class);
                        view.setBackground(createSubPbBackground());
                    }
                });
        // 楼层
        XposedHelpers.findAndHookConstructor("com.baidu.tieba.pb.pb.main.PbCommenFloorItemViewHolder", sClassLoader,
                XposedHelpers.findClass("com.baidu.tbadk.TbPageContext", sClassLoader), View.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ReflectUtils.walkObjectFields(param.thisObject, LinearLayout.class, objField -> {
                            LinearLayout ll = (LinearLayout) objField;
                            if (ll.getId() == ReflectUtils.getId("all_content")) {
                                ll.setBackground(createBackground());
                                return true;
                            }
                            return false;
                        });
                    }
                });
    }

    private StateListDrawable createBackground() {
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed},
                new ColorDrawable(ReflectUtils.getColor("CAM_X0204")));
        return sld;
    }

    private StateListDrawable createSubPbBackground() {
        if (!DisplayUtils.getTbSkin(getContext()).equals("")) {
            return createBackground();
        } else {
            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.WHITE));
            return sld;
        }
    }
}
