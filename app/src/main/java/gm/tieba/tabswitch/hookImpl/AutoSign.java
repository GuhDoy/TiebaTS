package gm.tieba.tabswitch.hookImpl;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class AutoSign extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                SharedPreferences tsConfig = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
                if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) != tsConfig.getInt("sign_date", 0))
                    new Thread(() -> {
                        Looper.prepare();
                        if (Hook.BDUSS == null)
                            Toast.makeText(activity.getApplicationContext(), "暂未获取到 BDUSS", Toast.LENGTH_LONG).show();
                        else {
                            String result = main(Hook.BDUSS);
                            Toast.makeText(activity.getApplicationContext(), result, Toast.LENGTH_LONG).show();
                            if (result.endsWith("全部签到成功")) {
                                SharedPreferences.Editor editor = tsConfig.edit();
                                editor.putInt("sign_date", Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
                                Hook.follow = new HashSet<>(success);
                                editor.putStringSet("follow", Hook.follow);
                                editor.apply();
                            }
                        }
                        Looper.loop();
                    }).start();
            }
        });
    }

    //获取用户所有关注贴吧
    private static final String LIKE_URL = "https://tieba.baidu.com/mo/q/newmoindex";
    //获取用户的tbs
    private static final String TBS_URL = "http://tieba.baidu.com/dc/common/tbs";
    //贴吧签到接口
    private static final String SIGN_URL = "http://c.tieba.baidu.com/c/c/forum/sign";

    private static final List<String> follow = new ArrayList<>();
    private static final List<String> success = new ArrayList<>();
    private static String tbs = "";
    private static Integer followNum = 201;

    private static String main(String arg) {
        AutoSignHelper.setCookie(arg);
        getTbs();
        getFollow();
        runSign();
        int failNum = followNum - success.size();
        String result = "共 {" + followNum + "} 个吧 - 成功: {" + success.size() + "} - 失败: {" + failNum + "}";
        XposedBridge.log(result);
        if (failNum == 0) return "共 {" + followNum + "} 个吧 - 全部签到成功";
        else return result;
    }

    private static void getTbs() {
        try {
            JSONObject jsonObject = AutoSignHelper.get(TBS_URL);
            if ("1".equals(jsonObject.getString("is_login"))) {
                XposedBridge.log("获取tbs成功");
                tbs = jsonObject.getString("tbs");
            } else XposedBridge.log("获取tbs失败 -- " + jsonObject);
        } catch (Exception e) {
            XposedBridge.log("获取tbs部分出现错误 -- " + e);
        }
    }

    private static void getFollow() {
        try {
            JSONObject jsonObject = AutoSignHelper.get(LIKE_URL);
            XposedBridge.log("获取贴吧列表成功");
            JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("like_forum");
            followNum = jsonArray.length();
            // 获取用户所有关注的贴吧
            for (int i = 0; i < jsonArray.length(); i++)
                if ("0".equals(jsonArray.optJSONObject(i).getString("is_sign")))
                    // 将未签到的贴吧加入到 follow 中，待签到
                    follow.add(jsonArray.optJSONObject(i).getString("forum_name"));
                else
                    // 将已经成功签到的贴吧，加入到 success
                    success.add(jsonArray.optJSONObject(i).getString("forum_name"));
        } catch (Exception e) {
            XposedBridge.log("获取贴吧列表部分出现错误 -- " + e);
        }
    }

    private static void runSign() {
        // 当执行 3 轮所有贴吧还未签到成功就结束操作
        int flag = 3;
        try {
            while (success.size() < followNum && flag > 0) {
                Iterator<String> iterator = follow.iterator();
                while (iterator.hasNext()) {
                    String s = iterator.next();
                    String body = "kw=" + s + "&tbs=" + tbs + "&sign=" + AutoSignHelper.enCodeMd5("kw=" + s + "tbs=" + tbs + "tiebaclient!!!");
                    JSONObject post = AutoSignHelper.post(SIGN_URL, body);
                    if ("0".equals(post.getString("error_code"))) {
                        iterator.remove();
                        success.add(s);
                        XposedBridge.log(s + ": " + "签到成功");
                    } else XposedBridge.log(s + ": " + "签到失败");
                }
                if (success.size() != followNum) {
                    Thread.sleep(1000);
                    getTbs();
                }
                flag--;
            }
        } catch (Exception e) {
            XposedBridge.log("签到部分出现错误 -- " + e);
        }
    }
}