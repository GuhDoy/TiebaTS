package gm.tieba.tabswitch.hooker;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
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
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.TSPreferenceHelper.SwitchButtonHolder;
import gm.tieba.tabswitch.hooker.add.MyAttention;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.Parser;
import gm.tieba.tabswitch.util.Reflect;
import gm.tieba.tabswitch.util.TraceChecker;
import gm.tieba.tabswitch.widget.NavigationBar;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbToast;

public class TSPreference extends BaseHooker implements IHooker {
    public final static String MAIN = "贴吧TS设置";
    public final static String MODIFY_TAB = "修改页面";
    public final static String NOTES = "备注关注的人";
    public final static String TRACE = "痕迹";
    private final static String PROXY_ACTIVITY = "com.baidu.tieba.setting.im.more.SecretSettingActivity";
    private static int sCount = 0;

    public TSPreference(ClassLoader classLoader, Context context, Resources res) {
        super(classLoader, context, res);
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
                        parent.addView(TSPreferenceHelper.createButton(MAIN, null,
                                v -> startMainPreferenceActivity(activity)), 11);
                    }
                });
        AcRules.findRule(sRes.getString(R.string.TSPreference), new AcRules.Callback() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) {
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, XposedHelpers
                        .findClass(PROXY_ACTIVITY, sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.args[0];
                        NavigationBar navigationBar = new NavigationBar(param.thisObject);
                        switch (activity.getIntent().getStringExtra("proxyPage")) {
                            case MAIN:
                                proxyPage(activity, navigationBar, MAIN, createMainPreference(activity));
                                break;
                            case MODIFY_TAB:
                                proxyPage(activity, navigationBar, MODIFY_TAB, createModifyTabPreference(activity));
                                break;
                            case NOTES:
                                proxyPage(activity, navigationBar, NOTES, MyAttention.createNotesPreference(activity));
                                break;
                            case TRACE:
                                proxyPage(activity, navigationBar, TRACE, createHidePreference(activity));
                                break;
                        }
                    }
                });
            }
        });
    }

    private void proxyPage(Activity activity, NavigationBar navigationBar, String title,
                           LinearLayout preferenceLayout) throws Throwable {
        navigationBar.setTitleText(title);
        navigationBar.addTextButton("重启", v -> DisplayHelper.restart(activity, sRes));
        LinearLayout containerView = activity.findViewById(Reflect.getId("container_view"));
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
                intent.setData(Uri.parse("package:" + (XposedInit.sPath.contains(BuildConfig.APPLICATION_ID)
                        && new File(XposedInit.sPath).exists() ?
                        BuildConfig.APPLICATION_ID : activity.getPackageName())));
                activity.startActivity(intent);
            });
            bdAlert.setOnYesButtonClickListener(v -> {
                Preferences.putEULAAccepted();
                startMainPreferenceActivity(activity);
                bdAlert.dismiss();
            });
            bdAlert.show();
        } else {
            Intent intent = new Intent().setClassName(activity, PROXY_ACTIVITY);
            intent.putExtra("proxyPage", MAIN);
            activity.startActivity(intent);
        }
    }

    @NotNull
    private LinearLayout createMainPreference(Activity activity) {
        boolean isPurifyEnabled = Preferences.getIsPurifyEnabled();
        TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurifyEnabled ? "轻车简从" : "净化界面"));
        preferenceLayout.addView(TSPreferenceHelper.createButton(MODIFY_TAB, null, v -> {
            Intent intent = new Intent().setClassName(activity, PROXY_ACTIVITY);
            intent.putExtra("proxyPage", MODIFY_TAB);
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

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurifyEnabled ? "别出新意" : "增加功能"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "进吧增加收藏、历史", "create_view", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "我的收藏增加搜索、吧名", "thread_store", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "浏览历史增加搜索", "history_cache", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "楼层回复增加查看主题贴", "new_sub", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "楼层增加点按效果", "ripple", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "长按下载保存全部图片", "save_images", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(TSPreferenceHelper.createButton(NOTES, null, v -> {
            Intent intent = new Intent().setClassName(activity, PROXY_ACTIVITY);
            intent.putExtra("proxyPage", NOTES);
            activity.startActivity(intent);
        }));

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurifyEnabled ? "垂手可得" : "自动化"));
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
        preferenceLayout.addView(new SwitchButtonHolder(activity, "自动切换夜间模式", "eyeshield_mode", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "自动查看原图", "origin_src", SwitchButtonHolder.TYPE_SWITCH));

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurifyEnabled ? "奇怪怪" : "其它"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "保存图片重定向", "redirect_image", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "禁用帖子手势", "forbid_gesture", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "用赞踩差数代替赞数", "agree_num", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "交换吧热门与最新", "frs_tab", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(TSPreferenceHelper.createButton(TRACE, "希望有一天不再需要贴吧TS", v -> {
            Intent intent = new Intent().setClassName(activity, PROXY_ACTIVITY);
            intent.putExtra("proxyPage", TRACE);
            activity.startActivity(intent);
        }));

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurifyEnabled ? "关于就是关于" : "关于"));
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
            if (!isPurifyEnabled && sCount >= 10) {
                Preferences.putPurifyEnabled();
                activity.recreate();
            }
        }));
        return preferenceLayout;
    }

    private LinearLayout createModifyTabPreference(Activity activity) {
        TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        preferenceLayout.addView(TSPreferenceHelper.createTextView("主页导航栏"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏首页", "home_recommend", SwitchButtonHolder.TYPE_SWITCH));
        for (String fieldName : Parser.parseMainTabActivityConfig()) {
            String text;
            switch (fieldName) {
                case "ENTER_FORUM_DELEGATE_AVAILABLE":
                    text = "隐藏进吧";
                    break;
                case "ENTER_FORUM_TAB_AVAIBLE":
                    text = "隐藏进吧（Flutter）";
                    break;
                case "NEW_CATEGORY_TAB_AVAIBLE":
                    text = "隐藏频道（Flutter）";
                    break;
                case "VIDEO_CHANNEL_TAB_AVAILABLE":
                    text = "隐藏视频";
                    break;
                case "IMMESSAGE_CENTER_DELEGATE_AVAIBLE":
                    text = "隐藏消息";
                    break;
                case "MEMBER_CENTER_TAB_AVAILABLE":
                    text = "隐藏会员中心";
                    break;
                case "IS_INDICATOR_BOTTOM":
                    text = "顶部导航栏";
                    break;
                default:
                    text = fieldName;
                    break;
            }
            preferenceLayout.addView(new SwitchButtonHolder(activity, text, fieldName, SwitchButtonHolder.TYPE_SET_MAIN));
        }
        preferenceLayout.addView(TSPreferenceHelper.createTextView("禁用 Flutter"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "我关注的吧", "flutter_concern_forum_enable_android", SwitchButtonHolder.TYPE_SET_FLUTTER));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "吧资料", "flutter_forum_detail_enable_android_112", SwitchButtonHolder.TYPE_SET_FLUTTER));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "MyTab", "flutter_mytab_enable_android_112", SwitchButtonHolder.TYPE_SET_FLUTTER));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "粉丝", "flutter_attention_enable_android_112", SwitchButtonHolder.TYPE_SET_FLUTTER));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "个人中心", "flutter_person_center_enable_android_12", SwitchButtonHolder.TYPE_SET_FLUTTER));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "一键签到", "flutter_signin_enable_android_119", SwitchButtonHolder.TYPE_SET_FLUTTER));
        return preferenceLayout;
    }

    private LinearLayout createHidePreference(Activity activity) {
        boolean isPurifyEnabled = Preferences.getIsPurifyEnabled();
        TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        if (isPurifyEnabled || BuildConfig.DEBUG) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurifyEnabled ? "尾巴是藏不住的（" : null));
            preferenceLayout.addView(new SwitchButtonHolder(activity, isPurifyEnabled ? "藏起尾巴" : "隐藏模块", "hide", SwitchButtonHolder.TYPE_SWITCH));
        }
        preferenceLayout.addView(TSPreferenceHelper.createTextView("检测设置"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "检测 Xposed", "check_xposed", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "检测模块", "check_module", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "检测堆栈（重启才能真正生效）", "check_stack_trace", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(TSPreferenceHelper.createButton(isPurifyEnabled ? "捏捏尾巴" : "检测模块", String.valueOf(Process.myPid()), v ->
                new TraceChecker(preferenceLayout).checkAll()));
        TraceChecker.sChildCount = preferenceLayout.getChildCount();
        return preferenceLayout;
    }
}
