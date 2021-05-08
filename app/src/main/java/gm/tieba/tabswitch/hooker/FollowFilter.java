package gm.tieba.tabswitch.hooker;

import android.os.Looper;
import android.widget.Toast;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.IHooker;
import gm.tieba.tabswitch.hooker.model.Preferences;

public class FollowFilter extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("tbclient.Personalized.DataRes$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (Preferences.getFollow() == null) {
                    Looper.prepare();
                    Toast.makeText(sContextRef.get(), "暂未获取到关注列表", Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
                List<?> list = (List<?>) XposedHelpers.getObjectField(param.thisObject, "thread_list");
                if (list == null) return;
                for (int i = 0; i < list.size(); i++) {
                    if (!Preferences.getFollow().contains(XposedHelpers.getObjectField(list.get(i), "fname"))) {
                        list.remove(i);
                        i--;
                    }
                }
            }
        });
    }
}