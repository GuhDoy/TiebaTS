package top.srcrs;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.Hook;
import top.srcrs.domain.Cookie;
import top.srcrs.util.Encryption;
import top.srcrs.util.Request;

/**
 * 程序运行开始的地方
 *
 * @author srcrs
 * @Time 2020-10-31
 */
public class Run extends Hook {
    //获取用户所有关注贴吧
    String LIKE_URL = "https://tieba.baidu.com/mo/q/newmoindex";
    //获取用户的tbs
    String TBS_URL = "http://tieba.baidu.com/dc/common/tbs";
    //贴吧签到接口
    String SIGN_URL = "http://c.tieba.baidu.com/c/c/forum/sign";

    //存储用户所关注的贴吧
    private List<String> follow = new ArrayList<>();
    //签到成功的贴吧列表
    public static List<String> success = new ArrayList<>();
    //用户的tbs
    private String tbs = "";
    //用户所关注的贴吧数量
    private static Integer followNum = 201;

    public static String main(String args) {
        Cookie cookie = Cookie.getInstance();
        // 存入Cookie，以备使用
        if (args == null) return null;
        cookie.setBDUSS(args);
        Run run = new Run();
        run.getTbs();
        run.getFollow();
        run.runSign();
        int failNum = followNum - success.size();
        String result = "共 {" + followNum + "} 个吧 - 成功: {" + success.size() + "} - 失败: {" + failNum + "}";
        XposedBridge.log(result);
        if (failNum == 0) return "共 {" + followNum + "} 个吧 - 全部签到成功";
        else return result;
    }

    /**
     * 进行登录，获得 tbs ，签到的时候需要用到这个参数
     *
     * @author srcrs
     * @Time 2020-10-31
     */
    public void getTbs() {
        try {
            com.alibaba.fastjson.JSONObject jsonObject = Request.get(TBS_URL);
            if ("1".equals(jsonObject.getString("is_login"))) {
                XposedBridge.log("获取tbs成功");
                tbs = jsonObject.getString("tbs");
            } else XposedBridge.log("获取tbs失败 -- " + jsonObject);
        } catch (Exception e) {
            XposedBridge.log("获取tbs部分出现错误 -- " + e);
        }
    }

    /**
     * 获取用户所关注的贴吧列表
     *
     * @author srcrs
     * @Time 2020-10-31
     */
    public void getFollow() {
        try {
            com.alibaba.fastjson.JSONObject jsonObject = Request.get(LIKE_URL);
            XposedBridge.log("获取贴吧列表成功");
            com.alibaba.fastjson.JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("like_forum");
            followNum = jsonArray.size();
            // 获取用户所有关注的贴吧
            for (Object array : jsonArray) {
                if ("0".equals(((JSONObject) array).getString("is_sign")))
                    // 将为签到的贴吧加入到 follow 中，待签到
                    follow.add(((JSONObject) array).getString("forum_name"));
                else
                    // 将已经成功签到的贴吧，加入到 success
                    success.add(((JSONObject) array).getString("forum_name"));
            }
        } catch (Exception e) {
            XposedBridge.log("获取贴吧列表部分出现错误 -- " + e);
        }
    }

    /**
     * 开始进行签到，每一轮性将所有未签到的贴吧进行签到，一共进行5轮，如果还未签到完就立即结束
     * 一般一次只会有少数的贴吧未能完成签到，为了减少接口访问次数，每一轮签到完等待1分钟，如果在过程中所有贴吧签到完则结束。
     *
     * @author srcrs
     * @Time 2020-10-31
     */
    public void runSign() {
        // 当执行 3 轮所有贴吧还未签到成功就结束操作
        int flag = 3;
        try {
            while (success.size() < followNum && flag > 0) {
                Iterator<String> iterator = follow.iterator();
                while (iterator.hasNext()) {
                    String s = iterator.next();
                    String body = "kw=" + s + "&tbs=" + tbs + "&sign=" + Encryption.enCodeMd5("kw=" + s + "tbs=" + tbs + "tiebaclient!!!");
                    com.alibaba.fastjson.JSONObject post = Request.post(SIGN_URL, body);
                    if ("0".equals(post.getString("error_code"))) {
                        iterator.remove();
                        success.add(s);
                        XposedBridge.log(s + ": " + "签到成功");
                    } else XposedBridge.log(s + ": " + "签到失败");
                }
                /*
                if (success.size() != followNum) {
                    // 为防止短时间内多次请求接口，触发风控，设置每一轮签到完等待 5 分钟
                    Thread.sleep(0);
                    // 重新获取 tbs，尝试解决以前第 1 次签到失败，剩余 4 次循环都会失败的错误。
                    getTbs();
                }
                */
                flag--;
            }
        } catch (Exception e) {
            XposedBridge.log("签到部分出现错误 -- " + e);
        }
    }
}