package gm.tieba.tabswitch.hooker.add;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
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
                    view.setBackground(createSubPbBackground(DisplayUtils.dipToPx(getContext(), 5F)));
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
                view.setBackground(createSubPbBackground(DisplayUtils.dipToPx(getContext(), 3.5F)));
            }
        });
    }

    private StateListDrawable createSubPbBackground(int bottomInset) {
        final StateListDrawable sld = new StateListDrawable();

        PaintDrawable bg = new PaintDrawable(Color.argb(192,
                Color.red(ReflectUtils.getColor("CAM_X0201")),
                Color.green(ReflectUtils.getColor("CAM_X0201")),
                Color.blue(ReflectUtils.getColor("CAM_X0201"))
        ));
        bg.setCornerRadius(DisplayUtils.dipToPx(getContext(), 2F));

        LayerDrawable layerBg = new LayerDrawable(new Drawable[]{bg});
        layerBg.setLayerInset(0, 0, 0, 0, bottomInset);

        sld.addState(new int[]{android.R.attr.state_pressed}, layerBg);
        return sld;
    }
}
