package gm.tieba.tabswitch.hooker.auto;


import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;

public class OriginSrc extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "origin_src";
    }

    private static void doHook() {
        AcRules.findRule(new StringMatcher("pic_amount"), (matcher, clazz, method) ->
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, JSONObject.class, Boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                        final JSONObject jsonObject = (JSONObject) param.args[0];
                        final JSONArray picList = jsonObject.optJSONArray("pic_list");
                        if (picList == null) return;
                        for (int i = 0; i < picList.length(); i++) {
                            final JSONObject pic = picList.optJSONObject(i);
                            final JSONObject img = pic.getJSONObject("img");
                            final JSONObject original = img.getJSONObject("original");
                            original.put("big_cdn_src", original.getString("original_src"));
                            img.put("original", original);
                            pic.put("img", img);
                            pic.put("show_original_btn", 0);
                        }
                        jsonObject.put("pic_list", picList);
                    }
                }));
        XposedHelpers.findAndHookMethod("tbclient.PbContent$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject, "show_original_btn", 0);
                final String[] strings = new String[]{"big_cdn_src", "cdn_src", "cdn_src_active"};
                for (final String string : strings) {
                    XposedHelpers.setObjectField(param.thisObject, string, XposedHelpers
                            .getObjectField(param.thisObject, "origin_src"));
                }
            }
        });
        XposedHelpers.findAndHookMethod("tbclient.Media$Builder", sClassLoader, "build", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject, "show_original_btn", 0);
                final String[] strings = new String[]{"small_pic", "water_pic"};
                for (final String string : strings) {
                    XposedHelpers.setObjectField(param.thisObject, string, XposedHelpers
                            .getObjectField(param.thisObject, "big_pic"));
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void hook() throws Throwable {
        final NetworkCallbackImpl networkCallback = new NetworkCallbackImpl();
        final NetworkRequest.Builder builder = new NetworkRequest.Builder();
        final NetworkRequest request = builder.build();
        final ConnectivityManager connMgr = (ConnectivityManager) getContext().getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) connMgr.registerNetworkCallback(request, networkCallback);
    }

    private static class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
        NetworkCallbackImpl() {
        }

        @Override
        public void onCapabilitiesChanged(final Network network, final NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                doHook();
            }
        }
    }
}
