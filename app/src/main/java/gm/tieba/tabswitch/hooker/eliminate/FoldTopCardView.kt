package gm.tieba.tabswitch.hooker.eliminate

import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker

class FoldTopCardView : XposedContext(), IHooker {
    override fun key(): String {
        return "fold_top_card_view"
    }

    override fun hook() {
        // 总是折叠置顶帖
        findClass("com.baidu.tieba.forum.view.TopCardView").declaredMethods.filter { method ->
            method.returnType == Boolean::class.javaPrimitiveType &&
                    method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == MutableList::class.java &&
                    method.parameterTypes[1] == Boolean::class.javaPrimitiveType
        }.forEach { method ->
            hookReplaceMethod(method) { false }
        }
    }
}
