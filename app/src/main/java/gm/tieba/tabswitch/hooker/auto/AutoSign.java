package gm.tieba.tabswitch.hooker.auto;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.XposedWrapper;
import gm.tieba.tabswitch.dao.Adp;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.widget.TbToast;

public class AutoSign extends XposedWrapper implements IHooker {
    //获取用户所有关注贴吧
    private static final String LIKE_URL = "https://tieba.baidu.com/mo/q/newmoindex";
    //获取用户的tbs
    private static final String TBS_URL = "http://tieba.baidu.com/dc/common/tbs";
    //贴吧签到接口
    private static final String SIGN_URL = "http://c.tieba.baidu.com/c/c/forum/sign";
    private final List<String> mFollow = new ArrayList<>();
    private final List<String> mSuccess = new ArrayList<>();
    private String mTbs;
    private Integer mFollowNum = 201;

    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (Preferences.getIsSigned()) return;
                        new Thread(() -> {
                            String result = main(Adp.getInstance().BDUSS);
                            if (result.endsWith("全部签到成功")) {
                                Preferences.putSignDate();
                                Preferences.putLikeForum(new HashSet<>(mSuccess));
                            }
                            new Handler(Looper.getMainLooper()).post(() -> TbToast.showTbToast(
                                    result, TbToast.LENGTH_SHORT));
                        }).start();
                    }
                });
    }

    private String main(String BDUSS) {
        if (BDUSS == null) return "暂未获取到 BDUSS";
        AutoSignHelper.setCookie(BDUSS);
        getTbs();
        getFollow();
        runSign();
        int failNum = mFollowNum - mSuccess.size();
        String result = "共 {" + mFollowNum + "} 个吧 - 成功: {" + mSuccess.size() + "} - 失败: {" + failNum + "}";
        XposedBridge.log(result);
        if (failNum == 0) return "共 {" + mFollowNum + "} 个吧 - 全部签到成功";
        else return result;
    }

    private void getTbs() {
        mTbs = Adp.getInstance().tbs;
        if (mTbs != null) return;
        try {
            JSONObject jsonObject = AutoSignHelper.get(TBS_URL);
            if ("1".equals(jsonObject.getString("is_login"))) {
                XposedBridge.log("获取tbs成功");
                mTbs = jsonObject.getString("tbs");
            } else XposedBridge.log("获取tbs失败 -- " + jsonObject);
        } catch (Exception e) {
            XposedBridge.log("获取tbs部分出现错误 -- " + e);
        }
    }

    private void getFollow() {
        try {
            JSONObject jsonObject = AutoSignHelper.get(LIKE_URL);
            XposedBridge.log("获取贴吧列表成功");
            JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("like_forum");
            mFollowNum = jsonArray.length();
            // 获取用户所有关注的贴吧
            for (int i = 0; i < jsonArray.length(); i++) {
                if ("0".equals(jsonArray.optJSONObject(i).getString("is_sign"))) {
                    mFollow.add(jsonArray.optJSONObject(i).getString("forum_name"));
                } else {
                    mSuccess.add(jsonArray.optJSONObject(i).getString("forum_name"));
                }
            }
        } catch (Exception e) {
            XposedBridge.log("获取贴吧列表部分出现错误 -- " + e);
        }
    }

    private void runSign() {
        // 当执行 3 轮所有贴吧还未签到成功就结束操作
        int flag = 3;
        try {
            while (mSuccess.size() < mFollowNum && flag > 0) {
                Iterator<String> iterator = mFollow.iterator();
                while (iterator.hasNext()) {
                    String s = iterator.next();
                    String body = "kw=" + s + "&tbs=" + mTbs + "&sign=" + AutoSignHelper.enCodeMd5(
                            "kw=" + s + "tbs=" + mTbs + "tiebaclient!!!");
                    JSONObject post = AutoSignHelper.post(SIGN_URL, body);
                    if ("0".equals(post.getString("error_code"))) {
                        iterator.remove();
                        mSuccess.add(s);
                        XposedBridge.log(s + ": " + "签到成功");
                    } else XposedBridge.log(s + ": " + "签到失败");
                }
                if (mSuccess.size() != mFollowNum) {
                    Thread.sleep(2500);
                    getTbs();
                    if (flag == 2) {
                        mFollow.clear();
                        mSuccess.clear();
                        getFollow();
                    }
                }
                flag--;
            }
        } catch (Exception e) {
            XposedBridge.log("签到部分出现错误 -- " + e);
        }
    }
}
