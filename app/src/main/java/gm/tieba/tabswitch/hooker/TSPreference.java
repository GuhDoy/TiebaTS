package gm.tieba.tabswitch.hooker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.BuildConfig;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.XposedInit;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.dao.Rule;
import gm.tieba.tabswitch.hooker.TSPreferenceHelper.SwitchButtonHolder;
import gm.tieba.tabswitch.util.Reflect;
import gm.tieba.tabswitch.widget.NavigationBar;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbToast;

public class TSPreference extends BaseHooker implements IHooker {
    private static int sCount = 0;
    private final static String SETTINGS_MAIN = "贴吧TS设置";
    private final static String SETTINGS_MODIFY_TAB = "修改页面";

    public TSPreference(ClassLoader classLoader, Resources res) {
        super(classLoader, res);
    }

    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod(Dialog.class, "dismissDialog", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Dialog dialog = (Dialog) param.thisObject;
                if (dialog.isShowing()) {
                    View view = dialog.getWindow().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) view.getContext()
                                .getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getRootView().getWindowToken(), 0);
                    }
                }
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.setting.more.MoreActivity", sClassLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;
                        FrameLayout browseSetting = activity.findViewById(
                                Reflect.getId("browseSetting"));
                        LinearLayout parent = (LinearLayout) browseSetting.getParent();
                        parent.addView(TSPreferenceHelper.createButton(SETTINGS_MAIN, null,
                                v -> startMainPreferenceActivity(activity)), 11);
                    }
                });
        Rule.findRule(sRes.getString(R.string.TSPreference), new Rule.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, XposedHelpers.findClass(
                        "com.baidu.tieba.setting.im.more.SecretSettingActivity", sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.args[0];
                        NavigationBar navigationBar = new NavigationBar(param.thisObject);
                        if (activity.getIntent().getBooleanExtra("showTSPreference", false)) {
                            proxyPage(activity, navigationBar, SETTINGS_MAIN, createMainPreference(activity));
                        } else if (activity.getIntent().getBooleanExtra("showModifyTabPreference", false)) {
                            proxyPage(activity, navigationBar, SETTINGS_MODIFY_TAB, createModifyTabPreference(activity));
                        }
                    }
                });
            }
        });
    }

    private void proxyPage(Activity activity, NavigationBar navigationBar, String title,
                           LinearLayout preferenceLayout) throws Throwable {
        navigationBar.setTitleText(title);
        navigationBar.addTextButton("重启", v -> {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
            activity.finishAffinity();
            System.exit(0);
        });
        LinearLayout containerView = activity.findViewById(
                Reflect.getId("container_view"));
        containerView.removeAllViews();
        containerView.addView(preferenceLayout);
    }

    private void startMainPreferenceActivity(Activity activity) {
        if (!Preferences.getIsEULAAccepted()) {
            StringBuilder stringBuilder = new StringBuilder().append(sRes.getString(R.string.EULA));
            if (BuildConfig.VERSION_NAME.contains("alpha") || BuildConfig.VERSION_NAME.contains("beta")) {
                stringBuilder.append("\n\n").append(sRes.getString(R.string.dev_tip));
            }
            TbDialog bdAlert = new TbDialog(activity, "使用协议", stringBuilder.toString(), true, null);
            bdAlert.setOnNoButtonClickListener(v -> {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_DELETE);
                if (XposedInit.sPath.contains(BuildConfig.APPLICATION_ID) && new File(XposedInit.sPath).exists()) {
                    intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                } else {
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                }
                activity.startActivity(intent);
            });
            bdAlert.setOnYesButtonClickListener(v -> {
                Preferences.putEULAAccepted();
                startMainPreferenceActivity(activity);
                bdAlert.dismiss();
            });
            bdAlert.show();
        } else {
            Intent intent = new Intent().setClassName(activity,
                    "com.baidu.tieba.setting.im.more.SecretSettingActivity");
            intent.putExtra("showTSPreference", true);
            activity.startActivity(intent);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @NotNull
    private LinearLayout createMainPreference(Activity activity) {
        boolean isPurifyEnabled = Preferences.getIsPurifyEnabled();
        TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        if (isPurifyEnabled) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("轻车简从"));
        } else {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("净化界面"));
        }
        preferenceLayout.addView(TSPreferenceHelper.createButton(SETTINGS_MODIFY_TAB, null, v -> {
            Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.setting.im.more.SecretSettingActivity");
            intent.putExtra("showModifyTabPreference", true);
            activity.startActivity(intent);
        }));
        if (isPurifyEnabled) {
            preferenceLayout.addView(new SwitchButtonHolder(activity, "真正的净化界面", "purify", SwitchButtonHolder.TYPE_SWITCH));
        }
        preferenceLayout.addView(new SwitchButtonHolder(activity, "净化进吧", "purify_enter", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "净化我的", "purify_my", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏小红点", "red_tip", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "只推荐已关注的吧", "follow_filter", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "过滤首页推荐", "personalized_filter", SwitchButtonHolder.TYPE_DIALOG));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "过滤帖子回复", "content_filter", SwitchButtonHolder.TYPE_DIALOG));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "过滤吧页面", "frs_page_filter", SwitchButtonHolder.TYPE_DIALOG));
        if (isPurifyEnabled) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("别出新意"));
        } else {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("增加功能"));
        }
        preferenceLayout.addView(new SwitchButtonHolder(activity, "进吧增加收藏、历史", "create_view", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "我的收藏增加搜索、吧名", "thread_store", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "浏览历史增加搜索", "history_cache", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "楼层回复增加查看主题贴", "new_sub", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "楼层增加水波纹点按效果", "ripple", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "长按下载保存全部图片", "save_images", SwitchButtonHolder.TYPE_SWITCH));
        // preferenceLayout.addView(new SwitchViewHolder(sClassLoader, activity, sRes, "长按关注的人设置备注名", "my_attention"));
        if (isPurifyEnabled) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("垂手可得"));
        } else {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("自动化"));
        }
        SwitchButtonHolder autoSign = new SwitchButtonHolder(activity, "自动签到", "auto_sign", SwitchButtonHolder.TYPE_SWITCH);
        if (!Preferences.getIsAutoSignEnabled()) {
            autoSign.setOnButtonClickListener(v -> {
                TbDialog bdalert = new TbDialog(activity, "提示",
                        "这是一个需要网络请求并且有封号风险的功能，您需要自行承担使用此功能的风险，请谨慎使用！", true, null);
                bdalert.setOnNoButtonClickListener(v2 -> bdalert.dismiss());
                bdalert.setOnYesButtonClickListener(v2 -> {
                    Preferences.putAutoSignEnabled();
                    autoSign.bdSwitch.turnOn();
                    bdalert.dismiss();
                });
                bdalert.show();
            });
        }
        preferenceLayout.addView(autoSign);
        preferenceLayout.addView(new SwitchButtonHolder(activity, "自动打开一键签到", "open_sign", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "更新时清理缓存", "clean_dir", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "自动切换夜间模式", "eyeshield_mode", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "自动查看原图", "origin_src", SwitchButtonHolder.TYPE_SWITCH));
        if (isPurifyEnabled) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("奇怪怪"));
        } else {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("其它"));
        }
        preferenceLayout.addView(new SwitchButtonHolder(activity, "存储重定向", "storage_redirect", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "禁用帖子手势", "forbid_gesture", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "用赞踩差数代替赞数", "agree_num", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "交换吧热门与最新", "frs_tab", SwitchButtonHolder.TYPE_SWITCH));
        if (isPurifyEnabled) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("关于就是关于"));
        } else {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("关于"));
        }
        preferenceLayout.addView(TSPreferenceHelper.createButton("版本", BuildConfig.VERSION_NAME, null));
        preferenceLayout.addView(TSPreferenceHelper.createButton("源代码", "想要小星星", v -> {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://github.com/GuhDoy/TiebaTS"));
            activity.startActivity(intent);
        }));
        preferenceLayout.addView(TSPreferenceHelper.createButton("TG群", "及时获取更新", v -> {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://t.me/TabSwitch"));
            activity.startActivity(intent);
        }));
        preferenceLayout.addView(TSPreferenceHelper.createButton("作者", "developed by GM", v -> {
            sCount++;
            if (sCount % 3 == 0) {
                TbToast.showTbToast(TSPreferenceHelper.randomToast(), TbToast.LENGTH_SHORT);
            }
            if (sCount >= 10) {
                Preferences.putPurifyEnabled();
            }
        }));
        return preferenceLayout;
    }

    private LinearLayout createModifyTabPreference(Activity activity) {
        TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        preferenceLayout.addView(TSPreferenceHelper.createTextView("修改底栏"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏首页", "home_recommend", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏进吧", "enter_forum", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏频道", "new_category", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏消息", "my_message", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(TSPreferenceHelper.createTextView("禁用 Flutter"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "我关注的吧", "flutter_concern_forum_enable_android", SwitchButtonHolder.TYPE_SET));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "吧资料", "flutter_forum_detail_enable_android_112", SwitchButtonHolder.TYPE_SET));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "MyTab", "flutter_mytab_enable_android_112", SwitchButtonHolder.TYPE_SET));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "粉丝", "flutter_attention_enable_android_112", SwitchButtonHolder.TYPE_SET));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "个人中心", "flutter_person_center_enable_android_12", SwitchButtonHolder.TYPE_SET));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "一键签到", "flutter_signin_enable_android_119", SwitchButtonHolder.TYPE_SET));
        return preferenceLayout;
    }
}
