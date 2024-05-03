package gm.tieba.tabswitch.hooker.auto

import de.robv.android.xposed.XposedBridge
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object AutoSignHelper {
    private lateinit var sCookie: String
    fun setCookie(BDUSS: String) {
        sCookie = "BDUSS=$BDUSS"
    }

    fun get(url: String): JSONObject {
        val okHttpClient = OkHttpClient()
        val request: Request = Builder()
            .url(url)
            .get()
            .addHeader("connection", "keep-alive")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("charset", "UTF-8")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36"
            )
            .addHeader("Cookie", sCookie)
            .build()

        val respContent = try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                throw IOException("Response code: ${response.code}")
            }
        } catch (e: IOException) {
            XposedBridge.log("get请求错误 -- $e")
            null
        }

        return respContent?.let { JSONObject(it) } ?: JSONObject()
    }

    fun post(url: String, body: String): JSONObject {
        val mediaType = "text/x-markdown; charset=utf-8".toMediaTypeOrNull()
        val stringBody: RequestBody = body.toRequestBody(mediaType)

        val request: Request = Builder()
            .url(url)
            .post(stringBody)
            .addHeader("connection", "keep-alive")
            .addHeader("Host", "tieba.baidu.com")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("charset", "UTF-8")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36"
            )
            .addHeader("Cookie", sCookie)
            .build()

        val respContent = try {
            val response = OkHttpClient().newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                throw IOException("Response code: ${response.code}")
            }
        } catch (e: IOException) {
            XposedBridge.log("post请求错误 -- $e")
            null
        }

        return respContent?.let { JSONObject(it) } ?: JSONObject()
    }

    fun enCodeMd5(str: String): String {
        return try {
            // 生成一个MD5加密计算摘要
            val md = MessageDigest.getInstance("MD5")
            // 计算md5函数
            md.update(str.toByteArray(StandardCharsets.UTF_8))
            // digest()最后确定返回md5 hash值，返回值为8位字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            //一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方）
            BigInteger(1, md.digest()).toString(16)
        } catch (e: Exception) {
            XposedBridge.log("字符串进行MD5加密错误 -- $e")
            ""
        }
    }
}
