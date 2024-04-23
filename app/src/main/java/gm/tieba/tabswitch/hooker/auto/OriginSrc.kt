package gm.tieba.tabswitch.hooker.auto

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.dao.Preferences.getBoolean
import gm.tieba.tabswitch.hooker.IHooker
import org.json.JSONObject

class OriginSrc : XposedContext(), IHooker {
    override fun key(): String {
        return "origin_src"
    }

    @SuppressLint("MissingPermission")
    @Throws(Throwable::class)
    override fun hook() {
        if (getBoolean("origin_src_only_wifi")) {
            val networkCallback = NetworkCallbackImpl()
            val builder = NetworkRequest.Builder()
            val request = builder.build()
            val connMgr = getContext().getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager
            connMgr.registerNetworkCallback(request, networkCallback)
        } else {
            doHook()
        }
    }

    private class NetworkCallbackImpl : NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            ) {
                doHook()
            } else {
                doUnHook()
            }
        }
    }

    companion object {
        private var isHooked = false
        private var picListUnhook: XC_MethodHook.Unhook? = null
        private var pbContentUnhook: XC_MethodHook.Unhook? = null
        private var mediaUnhook: XC_MethodHook.Unhook? = null
        private var picInfoUnhook: XC_MethodHook.Unhook? = null
        private var feedPicComponentUnhook: XC_MethodHook.Unhook? = null
        private fun doHook() {
            if (isHooked) return

            findRule("pic_amount") { _, clazz, method ->
                picListUnhook = hookBeforeMethod(
                    clazz, method, JSONObject::class.java, Boolean::class.javaObjectType
                ) { param ->
                    val jsonObject = param.args[0] as JSONObject

                    jsonObject.optJSONArray("pic_list")?.let {
                        for (i in 0 until it.length()) {
                            val pic = it.optJSONObject(i)
                            val img = pic.getJSONObject("img")
                            val original = img.getJSONObject("original").apply {
                                put("big_cdn_src", getString("original_src"))
                            }
                            img.put("original", original)
                            pic.apply {
                                put("img", img)
                                put("show_original_btn", 0)
                            }
                        }
                        jsonObject.put("pic_list", it)
                    }
                }

                pbContentUnhook = hookBeforeMethod(
                    "tbclient.PbContent\$Builder",
                    "build", Boolean::class.javaPrimitiveType,
                ) { param ->
                    XposedHelpers.setObjectField(param.thisObject, "show_original_btn", 0)
                    arrayOf("big_cdn_src", "cdn_src", "cdn_src_active").forEach {
                        XposedHelpers.setObjectField(
                            param.thisObject,
                            it,
                            XposedHelpers.getObjectField(param.thisObject, "origin_src")
                        )
                    }
                }

                mediaUnhook = hookBeforeMethod(
                    "tbclient.Media\$Builder",
                    "build", Boolean::class.javaPrimitiveType,
                ) { param ->
                    XposedHelpers.setObjectField(param.thisObject, "show_original_btn", 0)
                    arrayOf("small_pic", "water_pic").forEach {
                        XposedHelpers.setObjectField(
                            param.thisObject,
                            it,
                            XposedHelpers.getObjectField(param.thisObject, "big_pic")
                        )
                    }
                }

                picInfoUnhook = hookBeforeMethod(
                    "tbclient.PicInfo\$Builder",
                    "build", Boolean::class.javaPrimitiveType,
                ) { param ->
                    arrayOf("small_pic_url", "big_pic_url").forEach {
                        XposedHelpers.setObjectField(
                            param.thisObject,
                            it,
                            XposedHelpers.getObjectField(param.thisObject, "origin_pic_url")
                        )
                    }
                }

                feedPicComponentUnhook = hookBeforeMethod(
                    "tbclient.FeedPicComponent\$Builder",
                    "build", Boolean::class.javaPrimitiveType,
                ) { param ->
                    val schema = XposedHelpers.getObjectField(param.thisObject, "schema") as String
                    val paramsJson = Uri.parse(schema).getQueryParameter("params")

                    paramsJson?.let {
                        val jsonObject = JSONObject(it)
                        val pageParams = jsonObject.getJSONObject("pageParams")
                        val picDataList = pageParams.getJSONArray("pic_data_list")
                        for (i in 0 until picDataList.length()) {
                            val picData = picDataList.getJSONObject(i)
                            val originPicUrl = picData.getString("origin_pic_url")
                            picData.apply {
                                put("big_pic_url", originPicUrl)
                                put("small_pic_url", originPicUrl)
                                put("is_show_origin_btn", 0)
                            }
                        }
                        val modifiedUri = "tiebaapp://router/portal?params=$jsonObject"
                        XposedHelpers.setObjectField(param.thisObject, "schema", modifiedUri)
                    }
                }

                isHooked = true
            }
        }

        private fun doUnHook() {
            if (!isHooked) return
            picListUnhook?.let {
                it.unhook()
                picListUnhook = null
            }
            pbContentUnhook?.let {
                it.unhook()
                pbContentUnhook = null
            }
            mediaUnhook?.let {
                it.unhook()
                mediaUnhook = null
            }
            picInfoUnhook?.let {
                it.unhook()
                picInfoUnhook = null
            }
            feedPicComponentUnhook?.let {
                it.unhook()
                feedPicComponentUnhook = null
            }
            isHooked = false

        }
    }
}