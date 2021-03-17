package gm.tieba.tabswitch.hookImpl;


import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class OriginSrc extends Hook {
    @SuppressLint("MissingPermission")
    public static void hook(ClassLoader classLoader, Context context) throws Throwable {
        NetworkCallbackImpl networkCallback = new NetworkCallbackImpl(classLoader);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        NetworkRequest request = builder.build();
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) connMgr.registerNetworkCallback(request, networkCallback);
    }

    private static class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
        ClassLoader classLoader;

        NetworkCallbackImpl(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
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
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField(param.thisObject, "show_original_btn", 0);
                        String[] strings = new String[]{"big_cdn_src", "cdn_src", "cdn_src_active"};
                        for (String string : strings)
                            XposedHelpers.setObjectField(param.thisObject, string, XposedHelpers.getObjectField(param.thisObject, "origin_src"));
                    }
                });
                XposedHelpers.findAndHookMethod("tbclient.Media$Builder", classLoader, "build", boolean.class, new XC_MethodHook() {
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField(param.thisObject, "show_original_btn", 0);
                        String[] strings = new String[]{"small_pic", "water_pic"};
                        for (String string : strings)
                            XposedHelpers.setObjectField(param.thisObject, string, XposedHelpers.getObjectField(param.thisObject, "big_pic"));
                    }
                });
            }
        }
    }
}