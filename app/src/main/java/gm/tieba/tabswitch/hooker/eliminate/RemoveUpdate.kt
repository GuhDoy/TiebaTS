package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;

public class RemoveUpdate extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "remove_update";
    }

    @Override
    public void hook() throws Throwable {
        //Lcom/baidu/tbadk/coreExtra/data/VersionData;->parserJson(Lorg/json/JSONObject;)V
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.coreExtra.data.VersionData", sClassLoader,
                "parserJson", JSONObject.class, XC_MethodReplacement.returnConstant(null));
    }
}
