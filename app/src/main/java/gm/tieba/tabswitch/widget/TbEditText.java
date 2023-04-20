package gm.tieba.tabswitch.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import gm.tieba.tabswitch.util.ReflectUtils;

@SuppressLint("AppCompatCustomView")
public class TbEditText extends EditText {
    public TbEditText(final Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        setTextColor(ReflectUtils.getColor("CAM_X0105"));
        setHintTextColor(ReflectUtils.getColor("CAM_X0108"));
        setTextSize(ReflectUtils.getDimenDip("fontsize36"));
        setBackgroundResource(ReflectUtils.getDrawableId("blue_rectangle_input_bg"));
        setMinWidth((int) ReflectUtils.getDimen("ds140"));
    }
}
