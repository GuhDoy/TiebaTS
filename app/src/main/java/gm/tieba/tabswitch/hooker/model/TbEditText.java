package gm.tieba.tabswitch.hooker.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.util.DisplayHelper;

@SuppressLint("AppCompatCustomView")
public class TbEditText extends EditText {
    private TbEditText(Context context) {
        super(context);
    }

    public TbEditText(ClassLoader classLoader, Context context, Resources res) {
        this(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setHintTextColor(res.getColor(R.color.colorProgress, null));
        if (!DisplayHelper.isLightMode(context)) {
            setTextColor(res.getColor(R.color.colorPrimary, null));
        }
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        try {
            setTextSize(DisplayHelper.px2Dip(context, context.getResources().getDimension(
                    classLoader.loadClass("com.baidu.tieba.R$dimen").getField("fontsize36").getInt(null))));
            setBackgroundResource(classLoader.loadClass("com.baidu.tieba.R$drawable")
                    .getField("blue_rectangle_input_bg").getInt(null));
            setMinWidth((int) context.getResources().getDimension(classLoader.loadClass(
                    "com.baidu.tieba.R$dimen").getField("ds140").getInt(null)));
        } catch (Throwable throwable) {
            XposedBridge.log(throwable);
        }
    }
}
