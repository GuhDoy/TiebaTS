package gm.tieba.tabswitch.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.util.Reflect;

@SuppressLint("AppCompatCustomView")
public class TbEditText extends EditText {
    public TbEditText(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        try {
            setTextColor(Reflect.getColor("CAM_X0105"));
            setHintTextColor(Reflect.getColor("CAM_X0108"));
            setTextSize(Reflect.getDimenDip("fontsize36"));
            setBackgroundResource(Reflect.getDrawable("blue_rectangle_input_bg"));
            setMinWidth((int) Reflect.getDimen("ds140"));
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }
}
