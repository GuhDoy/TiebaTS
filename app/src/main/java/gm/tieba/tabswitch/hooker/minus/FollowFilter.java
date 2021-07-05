package gm.tieba.tabswitch.hooker.minus;

import android.os.Looper;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.widget.TbToast;

public class FollowFilter extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", sClassLoader,
                "build", boolean.class, new XC_MethodHook() {
                    @Override
                    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Set<String> forums = Preferences.getLikeForum();
                        if (forums == null) {
                            Looper.prepare();
                            TbToast.showTbToast("暂未获取到关注列表", TbToast.LENGTH_LONG);
                            Looper.loop();
                            return;
                        }
                        List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                        if (list == null) return;
                        list.removeIf((Predicate<Object>) o -> !forums.contains(
                                (String) XposedHelpers.getObjectField(o, "fname")));
                    }
                });
    }
}
