package gm.tieba.tabswitch.hooker;

import android.os.Looper;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.widget.TbToast;

public class FollowFilter extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (Preferences.getFollow() == null) {
                    Looper.prepare();
                    TbToast.showTbToast("暂未获取到关注列表", TbToast.LENGTH_LONG);
                    Looper.loop();
                }
                List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (list == null) return;
                for (int i = 0; i < list.size(); i++) {
                    if (!Preferences.getFollow().contains((String) XposedHelpers.getObjectField(list.get(i), "fname"))) {
                        list.remove(i);
                        i--;
                    }
                }
            }
        });
    }
}
