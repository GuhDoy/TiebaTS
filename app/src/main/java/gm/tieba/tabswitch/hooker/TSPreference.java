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
                        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getRootView().getWindowToken(), 0);
                    }
                }
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.setting.more.MoreActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                FrameLayout browseSetting = activity.findViewById(sClassLoader.loadClass("com.baidu.tieba.R$id").getField("browseSetting").getInt(null));
                LinearLayout parent = (LinearLayout) browseSetting.getParent();
                parent.addView(TSPreferenceHelper.createButton(sClassLoader, activity, SETTINGS_MAIN, null, v -> startMainPreferenceActivity(activity)), 11);
            }
        });
        Rule.findRule(sRes.getString(R.string.TSPreference), new Rule.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, XposedHelpers.findClass("com.baidu.tieba.setting.im.more.SecretSettingActivity", sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.args[0];
                        NavigationBar navigationBar = new NavigationBar(sClassLoader, activity, param.thisObject);
                        LinearLayout containerView = activity.findViewById(sClassLoader.loadClass("com.baidu.tieba.R$id")
                                .getField("container_view").getInt(null));
                        if (activity.getIntent().getBooleanExtra("showTSPreference", false)) {
                            proxyPage(activity, navigationBar, SETTINGS_MAIN, containerView, createMainPreference(activity));
                        } else if (activity.getIntent().getBooleanExtra("showModifyTabPreference", false)) {
                            proxyPage(activity, navigationBar, SETTINGS_MODIFY_TAB, containerView, createModifyTabPreference(activity));
                        }
                    }
                });
            }
        });
    }

    private void proxyPage(Activity activity, NavigationBar navigationBar, String title,
                           LinearLayout containerView, LinearLayout preferenceLayout) {
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
        containerView.removeAllViews();
        containerView.addView(preferenceLayout);
    }

    private void startMainPreferenceActivity(Activity activity) {
        if (!Preferences.getIsEULAAccepted()) {
            StringBuilder stringBuilder = new StringBuilder().append("本模块开源免费，不会主动发起网络请求，不会上传任何用户数据，旨在技术交流。请勿将本模块用于商业或非法用途，由此产生的后果与开发者无关。\n若您不同意此协议，请立即卸载本模块！无论您以何种形式或方式使用本模块，皆视为您已同意此协议！");
            if (BuildConfig.VERSION_NAME.contains("alpha") || BuildConfig.VERSION_NAME.contains("beta")) {
                stringBuilder.append("\n\n提示：您当前安装的是非正式版本，可能含有较多错误，如果您希望得到更稳定的使用体验，建议您安装正式版本。");
            }
            TbDialog bdAlert = new TbDialog(sClassLoader, activity, sRes, "使用协议", stringBuilder.toString(), true, null);
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
            Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.setting.im.more.SecretSettingActivity");
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
            preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "轻车简从"));
        } else {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "净化界面"));
        }
        preferenceLayout.addView(TSPreferenceHelper.createButton(sClassLoader, activity, SETTINGS_MODIFY_TAB, null, v -> {
            Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.setting.im.more.SecretSettingActivity");
            intent.putExtra("showModifyTabPreference", true);
            activity.startActivity(intent);
        }));
        if (isPurifyEnabled) {
            preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "真正的净化界面", "purify", SwitchButtonHolder.TYPE_SWITCH));
        }
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "净化进吧", "purify_enter", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "净化我的", "purify_my", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "隐藏小红点", "red_tip", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "只推荐已关注的吧", "follow_filter", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "过滤首页推荐", "personalized_filter", SwitchButtonHolder.TYPE_DIALOG));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "过滤帖子回复", "content_filter", SwitchButtonHolder.TYPE_DIALOG));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "过滤吧页面", "frs_page_filter", SwitchButtonHolder.TYPE_DIALOG));
        if (isPurifyEnabled) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "别出新意"));
        } else {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "增加功能"));
        }
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "进吧增加收藏、历史", "create_view", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "我的收藏增加搜索、吧名", "thread_store", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "浏览历史增加搜索", "history_cache", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "楼层回复增加查看主题贴", "new_sub", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "楼层增加水波纹点按效果", "ripple", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "长按下载保存全部图片", "save_images", SwitchButtonHolder.TYPE_SWITCH));
        // preferenceLayout.addView(new SwitchViewHolder(sClassLoader, activity, sRes, "长按关注的人设置备注名", "my_attention"));
        if (isPurifyEnabled) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "垂手可得"));
        } else {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "自动化"));
        }
        SwitchButtonHolder autoSign = new SwitchButtonHolder(sClassLoader, activity, sRes, "自动签到", "auto_sign", SwitchButtonHolder.TYPE_SWITCH);
        if (!Preferences.getIsAutoSignEnabled()) {
            autoSign.setOnButtonClickListener(v -> {
                TbDialog bdalert = new TbDialog(sClassLoader, activity, sRes, "提示",
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
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "自动打开一键签到", "open_sign", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "更新时清理缓存", "clean_dir", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "自动切换夜间模式", "eyeshield_mode", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "自动查看原图", "origin_src", SwitchButtonHolder.TYPE_SWITCH));
        if (isPurifyEnabled) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "奇怪怪"));
        } else {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "其它"));
        }
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "存储重定向", "storage_redirect", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "禁用帖子手势", "forbid_gesture", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "用赞踩差数代替赞数", "agree_num", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "交换吧热门与最新", "frs_tab", SwitchButtonHolder.TYPE_SWITCH));
        if (isPurifyEnabled) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "关于就是关于"));
        } else {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "关于"));
        }
        preferenceLayout.addView(TSPreferenceHelper.createButton(sClassLoader, activity, "版本", BuildConfig.VERSION_NAME, null));
        preferenceLayout.addView(TSPreferenceHelper.createButton(sClassLoader, activity, "源代码", "想要小星星", v -> {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://github.com/GuhDoy/TiebaTS"));
            activity.startActivity(intent);
        }));
        preferenceLayout.addView(TSPreferenceHelper.createButton(sClassLoader, activity, "TG群", "及时获取更新", v -> {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://t.me/TabSwitch"));
            activity.startActivity(intent);
        }));
        preferenceLayout.addView(TSPreferenceHelper.createButton(sClassLoader, activity, "作者", "developed by GM", v -> {
            sCount++;
            if (sCount % 3 == 0) {
                TbToast.showTbToast(sClassLoader, activity, sRes, TSPreferenceHelper.randomToast(), TbToast.LENGTH_SHORT);
            }
            if (sCount >= 10) {
                Preferences.putPurifyEnabled();
            }
        }));
        return preferenceLayout;
    }

    private LinearLayout createModifyTabPreference(Activity activity) {
        TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "修改底栏"));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "隐藏首页", "home_recommend", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "隐藏进吧", "enter_forum", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "隐藏频道", "new_category", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "隐藏消息", "my_message", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(TSPreferenceHelper.createTextView(sClassLoader, activity, "禁用 Flutter"));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "我关注的吧", "flutter_concern_forum_enable_android", SwitchButtonHolder.TYPE_SET));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "吧资料", "flutter_forum_detail_enable_android_112", SwitchButtonHolder.TYPE_SET));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "MyTab", "flutter_mytab_enable_android_112", SwitchButtonHolder.TYPE_SET));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "粉丝", "flutter_attention_enable_android_112", SwitchButtonHolder.TYPE_SET));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "个人中心", "flutter_person_center_enable_android_12", SwitchButtonHolder.TYPE_SET));
        preferenceLayout.addView(new SwitchButtonHolder(sClassLoader, activity, sRes, "一键签到", "flutter_signin_enable_android_119", SwitchButtonHolder.TYPE_SET));
        return preferenceLayout;
    }
}
