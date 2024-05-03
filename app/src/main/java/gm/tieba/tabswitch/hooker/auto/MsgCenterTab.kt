package gm.tieba.tabswitch.hooker.auto

import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.util.setObjectField

class MsgCenterTab : XposedContext(), IHooker {
    override fun key(): String {
        return "msg_center_tab"
    }

    override fun hook() {
        findClass("com.baidu.tieba.immessagecenter.msgtab.ui.view.MsgCenterContainerView").declaredMethods.filter {
            it.parameterTypes.isEmpty() && it.returnType == Long::class.javaPrimitiveType
        }.forEach { method ->
            hookBeforeMethod(
                "com.baidu.tieba.immessagecenter.msgtab.ui.view.MsgCenterContainerView",
                method.name
            ) { param ->
                setObjectField(param.thisObject, Long::class.javaObjectType, -1L)
            }
        }
    }
}
