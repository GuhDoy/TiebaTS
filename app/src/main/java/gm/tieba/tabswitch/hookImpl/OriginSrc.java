package gm.tieba.tabswitch.hookImpl;


import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class OriginSrc extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            if (Objects.equals(map.get("rule"), "\"original_src\""))
                XposedHelpers.findAndHookMethod(map.get("class"), classLoader, map.get("method"), JSONObject.class, new XC_MethodHook() {
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        JSONObject jsonObject = (JSONObject) param.args[0];
                        jsonObject.put("show_original_btn", 0);
                        JSONObject img = jsonObject.getJSONObject("img");
                        JSONObject original = img.getJSONObject("original");
                        original.put("big_cdn_src", original.getString("original_src"));
                        img.put("original", original);
                        jsonObject.put("img", img);
                    }
                });
        }
        XposedHelpers.findAndHookMethod(XposedHelpers.findClass("tbclient.PbContent$Builder", classLoader), "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field[] fields = new Field[]{param.thisObject.getClass().getDeclaredField("big_cdn_src"),
                        param.thisObject.getClass().getDeclaredField("cdn_src"),
                        param.thisObject.getClass().getDeclaredField("cdn_src_active")};
                for (Field field : fields) {
                    field.setAccessible(true);
                    String url = (String) field.get(param.thisObject);
                    if (url == null) continue;
                    url = url.replaceAll("http://tiebapic.baidu.com/forum/.+/", "http://tiebapic.baidu.com/forum/pic/item/");
                    field.set(param.thisObject, url);
                }
            }
        });
    }
}