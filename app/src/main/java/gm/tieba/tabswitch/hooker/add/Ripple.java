package gm.tieba.tabswitch.hooker.add;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.util.ReflectUtils;

public class Ripple extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "ripple";
    }

    public void hook() throws Throwable {
        final var subPbLayoutClass = XposedHelpers.findClass("com.baidu.tieba.pb.pb.sub.SubPbLayout", sClassLoader);
        // 楼中楼
        try {
            Method md;
            try {
                md = subPbLayoutClass.getDeclaredFields()[4].getType().getDeclaredMethod("createView");
            } catch (final NoSuchMethodException e) {
                md = subPbLayoutClass.getDeclaredFields()[4].getType().getDeclaredMethod("b");
            }
            XposedBridge.hookMethod(md, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    final var newSubPbListItem = (View) param.getResult();
                    final var tag = (SparseArray) newSubPbListItem.getTag();
                    final var b = tag.valueAt(0);
                    // R.id.new_sub_pb_list_richText
                    final var view = (View) ReflectUtils.getObjectField(b, "com.baidu.tbadk.widget.richText.TbRichTextView");
                    view.setBackground(createSubPbBackground());
                }
            });
        } catch (final NoSuchMethodException e) {
            XposedBridge.log(e);
        }
        // 查看全部回复
        XposedHelpers.findAndHookConstructor(subPbLayoutClass, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final var view = ReflectUtils.getObjectField(param.thisObject, RelativeLayout.class);
                view.setBackground(createSubPbBackground());
            }
        });
        // 楼层
        XposedHelpers.findAndHookConstructor("com.baidu.tieba.pb.pb.main.PbCommenFloorItemViewHolder", sClassLoader,
                XposedHelpers.findClass("com.baidu.tbadk.TbPageContext", sClassLoader), View.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        // R.id.all_content
                        final var mAllContent = ReflectUtils.getObjectField(param.thisObject, LinearLayout.class);
                        mAllContent.setBackground(createBackground());
                    }
                });
    }

    private StateListDrawable createBackground() {
        final StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed},
                new ColorDrawable(ReflectUtils.getColor("CAM_X0204")));
        return sld;
    }

    private StateListDrawable createSubPbBackground() {
        if (!DisplayUtils.getTbSkin(getContext()).equals("")) {
            return createBackground();
        } else {
            final StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.WHITE));
            return sld;
        }
    }
}
