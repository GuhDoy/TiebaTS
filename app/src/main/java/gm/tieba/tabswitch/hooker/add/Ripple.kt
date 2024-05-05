package gm.tieba.tabswitch.hooker.add

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.widget.RelativeLayout
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.util.dipToPx
import gm.tieba.tabswitch.util.getColor
import gm.tieba.tabswitch.util.getObjectField
import java.lang.reflect.Method

class Ripple : XposedContext(), IHooker {

    override fun key(): String {
        return "ripple"
    }

    override fun hook() {

        val subPbLayoutClass = findClass("com.baidu.tieba.pb.pb.sub.SubPbLayout")

        // 楼中楼
        val md: Method = try {
            subPbLayoutClass.declaredFields[4].type.getDeclaredMethod("createView")
        } catch (e: NoSuchMethodException) {
            subPbLayoutClass.declaredFields[4].type.getDeclaredMethod("b")
        }

        hookAfterMethod(md) { param ->
            val newSubPbListItem = param.result as View
            val tag = newSubPbListItem.tag as SparseArray<*>
            val b = tag.valueAt(0)
            // R.id.new_sub_pb_list_richText
            val view =
                getObjectField(b, "com.baidu.tbadk.widget.richText.TbRichTextView") as? View
            view?.background = createSubPbBackground(dipToPx(getContext(), 5f))
        }

        // 查看全部回复
        hookAfterConstructor(
            subPbLayoutClass,
            Context::class.java, AttributeSet::class.java,
        ) { param ->
            getObjectField(param.thisObject, RelativeLayout::class.java)
                ?.background = createSubPbBackground(dipToPx(getContext(), 3.5f))
        }
    }

    private fun createSubPbBackground(bottomInset: Int): StateListDrawable {
        val sld = StateListDrawable()
        val color = getColor("CAM_X0201")

        val bg = PaintDrawable(Color.argb(192, Color.red(color), Color.green(color), Color.blue(color)))
        bg.setCornerRadius(dipToPx(getContext(), 2f).toFloat())

        val layerBg = LayerDrawable(arrayOf<Drawable>(bg))
        layerBg.setLayerInset(0, 0, 0, 0, bottomInset)

        sld.addState(intArrayOf(android.R.attr.state_pressed), layerBg)
        return sld
    }
}