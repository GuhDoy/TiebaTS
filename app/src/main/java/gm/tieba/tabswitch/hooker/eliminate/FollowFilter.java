package gm.tieba.tabswitch.hooker.eliminate;

import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.widget.TbToast;

public class FollowFilter extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "follow_filter";
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", sClassLoader,
                "build", boolean.class, new XC_MethodHook() {
                    @Override
                    public void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                        final Set<String> forums = Preferences.getLikeForum();
                        if (forums == null) {
                            Looper.prepare();
                            TbToast.showTbToast("暂未获取到关注列表", TbToast.LENGTH_LONG);
                            Looper.loop();
                            return;
                        }
                        final List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                        if (list == null) return;
                        list.removeIf(o -> !forums.contains((String) XposedHelpers.getObjectField(o, "fname")));
                    }
                });
    }
}
