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
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.dao.Preferences.getBoolean
import gm.tieba.tabswitch.hooker.IHooker
import org.json.JSONException
import org.json.JSONObject

class OriginSrc : XposedContext(), IHooker {

    override fun key(): String {
        return "origin_src"
    }

    @SuppressLint("MissingPermission")
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

        private val unhookList = mutableListOf<XC_MethodHook.Unhook>()

        private fun doHook() {
            if (unhookList.isNotEmpty()) return

            findRule("pic_amount") { _, clazz, method ->
                unhookList.add(hookBeforeMethod(
                    clazz, method, JSONObject::class.java, Boolean::class.javaObjectType
                ) { param ->
                    val jsonObject = param.args[0] as JSONObject

                    jsonObject.optJSONArray("pic_list")?.let { picList ->
                        for (i in 0 until picList.length()) {
                            val pic = picList.optJSONObject(i)
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
                        jsonObject.put("pic_list", picList)
                    }
                })

                unhookList.add(hookBeforeMethod(
                    "tbclient.PbContent\$Builder",
                    "build", Boolean::class.javaPrimitiveType,
                ) { param ->
                    XposedHelpers.setObjectField(param.thisObject, "show_original_btn", 0)
                    arrayOf("big_cdn_src", "cdn_src", "cdn_src_active").forEach { field ->
                        XposedHelpers.setObjectField(
                            param.thisObject,
                            field,
                            XposedHelpers.getObjectField(param.thisObject, "origin_src")
                        )
                    }
                })

                unhookList.add(hookBeforeMethod(
                    "tbclient.Media\$Builder",
                    "build", Boolean::class.javaPrimitiveType,
                ) { param ->
                    XposedHelpers.setObjectField(param.thisObject, "show_original_btn", 0)
                    arrayOf("small_pic", "water_pic").forEach { field ->
                        XposedHelpers.setObjectField(
                            param.thisObject,
                            field,
                            XposedHelpers.getObjectField(param.thisObject, "big_pic")
                        )
                    }
                })

                unhookList.add(hookBeforeMethod(
                    "tbclient.PicInfo\$Builder",
                    "build", Boolean::class.javaPrimitiveType,
                ) { param ->
                    arrayOf("small_pic_url", "big_pic_url").forEach { field ->
                        XposedHelpers.setObjectField(
                            param.thisObject,
                            field,
                            XposedHelpers.getObjectField(param.thisObject, "origin_pic_url")
                        )
                    }
                })

                unhookList.add(hookBeforeMethod(
                    "tbclient.FeedPicComponent\$Builder",
                    "build", Boolean::class.javaPrimitiveType,
                ) { param ->
                    val schema = XposedHelpers.getObjectField(param.thisObject, "schema") as? String
                    val paramsJson = schema?.let { Uri.parse(it).getQueryParameter("params") }

                    paramsJson?.let { schemaParams ->
                        val jsonObject = JSONObject(schemaParams)
                        try {
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
                        } catch (ignored: JSONException) {
                        }
                    }
                })
            }
        }

        private fun doUnHook() {
            if (unhookList.isEmpty()) return
            unhookList.forEach { it.unhook() }
            unhookList.clear()
        }
    }
}