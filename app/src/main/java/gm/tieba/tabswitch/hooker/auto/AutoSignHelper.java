package gm.tieba.tabswitch.hooker.auto;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import de.robv.android.xposed.XposedBridge;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AutoSignHelper {
    private static String sCookie;

    static void setCookie(final String BDUSS) {
        sCookie = "BDUSS=" + BDUSS;
    }

    static JSONObject get(final String url) throws JSONException {
        final OkHttpClient okHttpClient = new OkHttpClient();
        final okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url).get()
                .addHeader("connection", "keep-alive")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("charset", "UTF-8")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36")
                .addHeader("Cookie", sCookie)
                .build();
        final Call call = okHttpClient.newCall(request);
        String respContent = null;
        try {
            final Response response = call.execute();
            if (response.code() < 400) respContent = response.body().string();
            else throw new IOException("response code: " + response.code());
        } catch (final IOException e) {
            XposedBridge.log("get请求错误 -- " + e);
        }
        return new JSONObject(respContent);
    }

    static JSONObject post(final String url, final String body) throws JSONException {
        final MediaType mediaType = MediaType.Companion.parse("text/x-markdown; charset=utf-8");
        final RequestBody stringBody = RequestBody.Companion.create(body, mediaType);
        final OkHttpClient okHttpClient = new OkHttpClient();
        final okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url).post(stringBody)
                .addHeader("connection", "keep-alive")
                .addHeader("Host", "tieba.baidu.com")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("charset", "UTF-8")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36")
                .addHeader("Cookie", sCookie)
                .build();
        final Call call = okHttpClient.newCall(request);
        String respContent = null;
        try {
            final Response response = call.execute();
            if (response.code() < 400) respContent = response.body().string();
            else throw new IOException("response code: " + response.code());
        } catch (final IOException e) {
            XposedBridge.log("post请求错误 -- " + e);
        }
        return new JSONObject(respContent);
    }

    static String enCodeMd5(final String str) {
        try {
            // 生成一个MD5加密计算摘要
            final MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(str.getBytes(StandardCharsets.UTF_8));
            // digest()最后确定返回md5 hash值，返回值为8位字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            //一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方）
            return new BigInteger(1, md.digest()).toString(16);
        } catch (final Exception e) {
            XposedBridge.log("字符串进行MD5加密错误 -- " + e);
            return "";
        }
    }
}
