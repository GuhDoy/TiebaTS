package gm.tieba.tabswitch.hooker.eliminate

import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.hooker.IHooker
import org.json.JSONObject

class RemoveUpdate : XposedContext(), IHooker {

    override fun key(): String {
        return "remove_update"
    }

    override fun hook() {
        //Lcom/baidu/tbadk/coreExtra/data/VersionData;->parserJson(Lorg/json/JSONObject;)V
        hookReplaceMethod(
            "com.baidu.tbadk.coreExtra.data.VersionData",
            "parserJson", JSONObject::class.java
        ) { null }
    }
}
