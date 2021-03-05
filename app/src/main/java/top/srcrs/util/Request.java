package top.srcrs.util;


import com.alibaba.fastjson.JSONObject;

import java.io.IOException;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.Hook;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import top.srcrs.domain.Cookie;

/**
 * 封装的网络请求请求工具类
 *
 * @author srcrs
 * @Time 2020-10-31
 */
public class Request extends Hook {
    /**
     * 获取Cookie对象
     */
    private static Cookie cookie = Cookie.getInstance();

    private Request() {
    }

    /**
     * 发送get请求
     *
     * @param url 请求的地址，包括参数
     * @return JSONObject
     * @author srcrs
     * @Time 2020-10-31
     */
    public static JSONObject get(String url) {
        OkHttpClient okHttpClient = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url).get()
                .addHeader("connection", "keep-alive")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("charset", "UTF-8")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36")
                .addHeader("Cookie", cookie.getCookie())
                .build();
        Call call = okHttpClient.newCall(request);
        String respContent = null;
        try {
            Response response = call.execute();
            if (response.code() < 400) respContent = response.body().string();
            else throw new IOException("response code: " + response.code());
        } catch (IOException e) {
            XposedBridge.log("get请求错误 -- " + e);
        }
        return JSONObject.parseObject(respContent);
    }

    /**
     * 发送post请求
     *
     * @param url  请求的地址
     * @param body 携带的参数
     * @return JSONObject
     * @author srcrs
     * @Time 2020-10-31
     */
    public static JSONObject post(String url, String body) {
        MediaType mediaType = MediaType.Companion.parse("text/x-markdown; charset=utf-8");
        RequestBody stringBody = RequestBody.Companion.create(body, mediaType);
        OkHttpClient okHttpClient = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url).post(stringBody)
                .addHeader("connection", "keep-alive")
                .addHeader("Host", "tieba.baidu.com")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("charset", "UTF-8")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36")
                .addHeader("Cookie", cookie.getCookie())
                .build();
        Call call = okHttpClient.newCall(request);
        String respContent = null;
        try {
            Response response = call.execute();
            if (response.code() < 400) respContent = response.body().string();
            else throw new IOException("response code: " + response.code());
        } catch (IOException e) {
            XposedBridge.log("post请求错误 -- " + e);
        }
        return JSONObject.parseObject(respContent);
    }
}