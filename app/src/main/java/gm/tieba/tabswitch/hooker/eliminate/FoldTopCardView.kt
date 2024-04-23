package gm.tieba.tabswitch.hooker.eliminate;

import androidx.annotation.NonNull;

import java.util.List;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;

public class FoldTopCardView extends XposedContext implements IHooker {
    @NonNull
    @Override
    public String key() {
        return "fold_top_card_view";
    }

    @Override
    public void hook() throws Throwable {
        // 总是折叠置顶帖
        for (final var method : XposedHelpers.findClass("com.baidu.tieba.forum.view.TopCardView", sClassLoader).getDeclaredMethods()) {
            if (method.getReturnType() == boolean.class) {
                final var currMethodParameterTypes = method.getParameterTypes();
                if (currMethodParameterTypes.length == 2 && currMethodParameterTypes[0] == List.class && currMethodParameterTypes[1] == boolean.class) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false));
                }
            }
        }
    }
}
