package gm.tieba.tabswitch.hookImpl;


import org.json.JSONArray;
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
            if (Objects.equals(map.get("rule"), "\"pic_amount\""))
                XposedHelpers.findAndHookMethod(map.get("class"), classLoader, map.get("method"), JSONObject.class, Boolean.class, new XC_MethodHook() {
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        JSONObject jsonObject = (JSONObject) param.args[0];
                        JSONArray picList = jsonObject.optJSONArray("pic_list");
                        if (picList == null) return;
                        for (int i = 0; i < picList.length(); i++) {
                            JSONObject pic = picList.optJSONObject(i);
                            JSONObject img = pic.getJSONObject("img");
                            JSONObject original = img.getJSONObject("original");
                            original.put("big_cdn_src", original.getString("original_src"));
                            img.put("original", original);
                            pic.put("img", img);
                            pic.put("show_original_btn", 0);
                        }
                        jsonObject.put("pic_list", picList);
                    }
                });
        }
        XposedHelpers.findAndHookMethod("tbclient.PbContent$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field showOriginalBtn = param.thisObject.getClass().getDeclaredField("show_original_btn");
                showOriginalBtn.setAccessible(true);
                showOriginalBtn.set(param.thisObject, 0);
                Field originSrc = param.thisObject.getClass().getDeclaredField("origin_src");
                originSrc.setAccessible(true);
                Field[] fields = new Field[]{param.thisObject.getClass().getDeclaredField("big_cdn_src"),
                        param.thisObject.getClass().getDeclaredField("cdn_src"),
                        param.thisObject.getClass().getDeclaredField("cdn_src_active")};
                for (Field mField : fields) {
                    mField.setAccessible(true);
                    mField.set(param.thisObject, originSrc.get(param.thisObject));
                }
            }
        });
        XposedHelpers.findAndHookMethod("tbclient.Media$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field showOriginalBtn = param.thisObject.getClass().getDeclaredField("show_original_btn");
                showOriginalBtn.setAccessible(true);
                showOriginalBtn.set(param.thisObject, 0);
            }
        });
    }
}