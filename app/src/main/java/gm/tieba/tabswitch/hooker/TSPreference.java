package gm.tieba.tabswitch.hooker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.luckypray.dexkit.query.matchers.ClassMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BuildConfig;
import gm.tieba.tabswitch.Constants;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.TSPreferenceHelper.SwitchButtonHolder;
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.SmaliMatcher;
import gm.tieba.tabswitch.hooker.extra.TraceChecker;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.widget.NavigationBar;
import gm.tieba.tabswitch.widget.TbToast;

public class TSPreference extends XposedContext implements IHooker, Obfuscated {
    public final static String MAIN = "贴吧TS设置";
    public final static String MODIFY_TAB = "修改页面";
    public final static String TRACE = "痕迹";
    private final static String PROXY_ACTIVITY = "com.baidu.tieba.setting.im.more.SecretSettingActivity";
    private static int sCount = 0;

    @NonNull
    @Override
    public String key() {
        return "ts_pref";
    }

    @Override
    public List<? extends Matcher> matchers() {
        return List.of(new SmaliMatcher(
                "Lcom/baidu/tbadk/data/MetaData;->getBazhuGradeData()Lcom/baidu/tbadk/coreExtra/data/BazhuGradeData;")
                        .setBaseClassMatcher(ClassMatcher.create().usingStrings("mo/q/wise-bawu-core/privacy-policy")
        ));
    }

    @Override
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod(Dialog.class, "dismissDialog", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) {
                final Dialog dialog = (Dialog) param.thisObject;
                if (dialog.isShowing()) {
                    final View view = dialog.getWindow().getCurrentFocus();
                    if (view != null) {
                        final InputMethodManager imm = (InputMethodManager) view.getContext()
                                .getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getRootView().getWindowToken(), 0);
                    }
                }
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.setting.more.MoreActivity", sClassLoader,
                "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        final var activity = (Activity) param.thisObject;
                        final var contentView = (ViewGroup) activity.findViewById(android.R.id.content);
                        final var parent = (RelativeLayout) contentView.getChildAt(0);
                        final var scroll = (ScrollView) parent.getChildAt(0);
                        final var containerView = (LinearLayout) scroll.getChildAt(0);
                        containerView.addView(TSPreferenceHelper.createButton(MAIN, null, true,
                                v -> startRootPreferenceActivity(activity)), 11);
                    }
                });
        AcRules.findRule(matchers(), (matcher, clazz, method) -> {
            try {
                XposedHelpers.findAndHookConstructor(clazz, sClassLoader, XposedHelpers
                        .findClass(PROXY_ACTIVITY, sClassLoader), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        final var activity = (Activity) param.args[0];
                        try {
                            final var navigationBar = new NavigationBar(param.thisObject);
                            final var proxyPage = activity.getIntent().getStringExtra("proxyPage");
                            if (proxyPage == null) return;
                            switch (proxyPage) {
                                case MAIN:
                                    proxyPage(activity, navigationBar, MAIN, createRootPreference(activity));
                                    break;
                                case MODIFY_TAB:
                                    proxyPage(activity, navigationBar, MODIFY_TAB, createModifyTabPreference(activity));
                                    break;
                                case TRACE:
                                    proxyPage(activity, navigationBar, TRACE, createHidePreference(activity));
                                    break;
                            }
                        } catch (final Throwable tr) {
                            final var messages = new ArrayList<String>();
                            messages.add(Constants.getStrings().get("exception_init_preference"));
                            messages.add(String.format(Locale.CHINA, "贴吧版本：%s, 模块版本：%d",
                                    DeobfuscationHelper.getTbVersion(getContext()), BuildConfig.VERSION_CODE));
                            messages.add(Log.getStackTraceString(tr));
                            final var message = TextUtils.join("\n", messages);
                            XposedBridge.log(message);
                            AlertDialog alert = new AlertDialog.Builder(activity, DisplayUtils.isLightMode(getContext()) ?
                                    android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                    .setTitle("规则异常").setMessage(message).setCancelable(false)
                                    .setPositiveButton(activity.getString(android.R.string.ok), (dialogInterface, i) -> activity.finish())
                                    .create();
                            alert.show();
                            DisplayUtils.fixAlertDialogWidth(alert);
                        }
                    }
                });
            } catch (final NoSuchMethodError ignored) {
            }
        });
    }

    private void proxyPage(final Activity activity, final NavigationBar navigationBar, final String title,
                           final LinearLayout preferenceLayout) throws Throwable {
        navigationBar.setTitleText(title);
        navigationBar.setCenterTextTitle("");
        navigationBar.addTextButton("重启", v -> {
            Preferences.commit();
            DisplayUtils.restart(activity);
        });
        final var contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        final var parent = (LinearLayout) contentView.getChildAt(0);
        final var mainScroll = (ScrollView) parent.getChildAt(1);
        final var containerView = (LinearLayout) mainScroll.getChildAt(0);
        containerView.removeAllViews();
        containerView.addView(preferenceLayout);
    }

    private void startRootPreferenceActivity(final Activity activity) {
        if (!Preferences.isEULAAccepted()) {
            final StringBuilder stringBuilder = new StringBuilder().append(Constants.getStrings().get("EULA"));
            if (isModuleBetaVersion) {
                stringBuilder.append("\n\n").append(Constants.getStrings().get("dev_tip"));
            }
            AlertDialog alert = new AlertDialog.Builder(activity, DisplayUtils.isLightMode(getContext()) ?
                    android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("使用协议").setMessage(stringBuilder.toString())
                    .setNegativeButton(activity.getString(android.R.string.cancel), null)
                    .setPositiveButton(activity.getString(android.R.string.ok), (dialogInterface, i) -> {
                        Preferences.putEULAAccepted();
                        startRootPreferenceActivity(activity);
                    })
                    .create();
            alert.show();
            DisplayUtils.fixAlertDialogWidth(alert);
        } else {
            final Intent intent = new Intent().setClassName(activity, PROXY_ACTIVITY);
            intent.putExtra("proxyPage", MAIN);
            activity.startActivity(intent);
        }
    }

    @NotNull
    private LinearLayout createRootPreference(final Activity activity) {
        final boolean isPurgeEnabled = Preferences.isPurgeEnabled();
        final TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurgeEnabled ? "轻车简从" : "净化界面"));
        preferenceLayout.addView(TSPreferenceHelper.createButton(MODIFY_TAB, null, true, v -> {
            final Intent intent = new Intent().setClassName(activity, PROXY_ACTIVITY);
            intent.putExtra("proxyPage", MODIFY_TAB);
            activity.startActivity(intent);
        }));
        if (isPurgeEnabled) {
            preferenceLayout.addView(new SwitchButtonHolder(activity, "真正的净化界面", "purge", SwitchButtonHolder.TYPE_SWITCH));
        }
        preferenceLayout.addView(new SwitchButtonHolder(activity, "净化进吧", "purge_enter", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "净化我的", "purge_my", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "默认折叠置顶帖", "fold_top_card_view", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "只推荐已关注的吧", "follow_filter", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "屏蔽首页视频贴", "purge_video", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "过滤首页推荐", "personalized_filter", SwitchButtonHolder.TYPE_DIALOG));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "过滤吧页面", "frs_page_filter", SwitchButtonHolder.TYPE_DIALOG));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "过滤帖子回复", "content_filter", SwitchButtonHolder.TYPE_DIALOG));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "过滤用户", "user_filter", SwitchButtonHolder.TYPE_DIALOG));

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurgeEnabled ? "别出新意" : "增加功能"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "浏览历史增加搜索", "history_cache", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "楼层增加点按效果", "ripple", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "长按下载保存全部图片", "save_images", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "弹窗自由复制", "select_clipboard", SwitchButtonHolder.TYPE_SWITCH));

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurgeEnabled ? "垂手可得" : "自动化"));
        final SwitchButtonHolder autoSign = new SwitchButtonHolder(activity, "自动签到", "auto_sign", SwitchButtonHolder.TYPE_SWITCH);
        autoSign.setOnButtonClickListener(v -> {
            if (!Preferences.isAutoSignEnabled()) {
                AlertDialog alert = new AlertDialog.Builder(activity, DisplayUtils.isLightMode(getContext()) ?
                        android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle("提示").setMessage("这是一个需要网络请求并且有封号风险的功能，您需要自行承担使用此功能的风险，请谨慎使用！")
                        .setNegativeButton(activity.getString(android.R.string.cancel), null)
                        .setPositiveButton(activity.getString(android.R.string.ok), (dialogInterface, i) -> {
                            Preferences.putAutoSignEnabled();
                            autoSign.bdSwitch.turnOn();
                        })
                        .create();
                alert.show();
                DisplayUtils.fixAlertDialogWidth(alert);
            } else {
                autoSign.bdSwitch.changeState();
            }
        });
        preferenceLayout.addView(autoSign);
        preferenceLayout.addView(new SwitchButtonHolder(activity, "自动打开一键签到", "open_sign", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "吧页面起始页面改为最新", "frs_tab", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "消息页面起始页面改为通知", "msg_center_tab", SwitchButtonHolder.TYPE_SWITCH));

        var originSrcOnlyWifiButton = new SwitchButtonHolder(activity, "自动查看原图仅WiFi下生效", "origin_src_only_wifi", SwitchButtonHolder.TYPE_SWITCH);
        var originSrcButton = new SwitchButtonHolder(activity, "自动查看原图", "origin_src", SwitchButtonHolder.TYPE_SWITCH);
        originSrcButton.setOnButtonClickListener(v -> {
                    originSrcButton.bdSwitch.changeState();
                    originSrcOnlyWifiButton.switchButton.setVisibility(Preferences.getBoolean("origin_src") ? View.VISIBLE : View.GONE);
                }
        );
        originSrcOnlyWifiButton.switchButton.setVisibility(Preferences.getBoolean("origin_src") ? View.VISIBLE : View.GONE);

        preferenceLayout.addView(originSrcButton);
        preferenceLayout.addView(originSrcOnlyWifiButton);

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurgeEnabled ? "奇怪怪" : "其它"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏小红点", "red_tip", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "禁用更新提示", "remove_update", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "禁用帖子手势", "forbid_gesture", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "用赞踩差数代替赞数", "agree_num", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "禁止检测通知开启状态", "notification_detect", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(TSPreferenceHelper.createButton(TRACE, "希望有一天不再需要贴吧TS", true, v -> {
            final Intent intent = new Intent().setClassName(activity, PROXY_ACTIVITY);
            intent.putExtra("proxyPage", TRACE);
            activity.startActivity(intent);
        }));

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurgeEnabled ? "关于就是关于" : "关于"));
        preferenceLayout.addView(TSPreferenceHelper.createButton("作者", "GM", true, v -> {
            sCount++;
            if (sCount % 3 == 0) {
                TbToast.showTbToast(TSPreferenceHelper.randomToast(), TbToast.LENGTH_SHORT);
            }
            if (!isPurgeEnabled && sCount >= 10) {
                Preferences.putPurgeEnabled();
                activity.recreate();
            }
        }));
        preferenceLayout.addView(TSPreferenceHelper.createButton("源代码", "想要小星星", true, v -> {
            final Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://github.com/GuhDoy/TiebaTS"));
            activity.startActivity(intent);
        }));
        preferenceLayout.addView(TSPreferenceHelper.createButton("TG群", "及时获取更新", true, v -> {
            final Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://t.me/TabSwitch"));
            activity.startActivity(intent);
        }));
        preferenceLayout.addView(TSPreferenceHelper.createButton("版本", String.format(Locale.CHINA, "%s", BuildConfig.VERSION_NAME), true, v -> {
            final Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            if (isModuleBetaVersion) {
                intent.setData(Uri.parse(Constants.getStrings().get("ci_uri")));
            } else {
                intent.setData(Uri.parse(Constants.getStrings().get("release_uri")));
            }
            activity.startActivity(intent);
        }));
        return preferenceLayout;
    }

    private LinearLayout createModifyTabPreference(final Activity activity) {
        final TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        preferenceLayout.addView(TSPreferenceHelper.createTextView("主页导航栏"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏首页", "home_recommend", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏进吧", "enter_forum", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏发帖", "write_thread", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏消息", "im_message", SwitchButtonHolder.TYPE_SWITCH));

        preferenceLayout.addView(TSPreferenceHelper.createTextView("其他"));
        SwitchButtonHolder transitionAnimation = new SwitchButtonHolder(activity, "修复过渡动画", "transition_animation", SwitchButtonHolder.TYPE_SWITCH);

        boolean shouldEnableTransitionAnimationFix = Build.VERSION.SDK_INT >= 34 && DeobfuscationHelper.isTbSatisfyVersionRequirement("12.58.2.1");
        if (!shouldEnableTransitionAnimationFix && Preferences.getTransitionAnimationEnabled()) {
            transitionAnimation.bdSwitch.turnOff();
        }

        transitionAnimation.setOnButtonClickListener(v -> {
            if (!shouldEnableTransitionAnimationFix) {
                TbToast.showTbToast("当前贴吧版本不支持此功能", TbToast.LENGTH_SHORT);
            } else {
                transitionAnimation.bdSwitch.changeState();
            }
        });
        preferenceLayout.addView(transitionAnimation);

        return preferenceLayout;
    }

    private LinearLayout createHidePreference(final Activity activity) {
        final boolean isPurgeEnabled = Preferences.isPurgeEnabled();
        final TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        if (isPurgeEnabled || BuildConfig.DEBUG) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView("隐藏设置"));
            preferenceLayout.addView(new SwitchButtonHolder(activity, isPurgeEnabled ? "藏起尾巴" : "隐藏模块", "hide", SwitchButtonHolder.TYPE_SWITCH));
            preferenceLayout.addView(new SwitchButtonHolder(activity, isPurgeEnabled ? "藏起尾巴（原生）" : "隐藏模块（原生）", "hide_native", SwitchButtonHolder.TYPE_SWITCH));
        }
        preferenceLayout.addView(TSPreferenceHelper.createTextView("检测设置"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "检测 Xposed", "check_xposed", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "检测模块", "check_module", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "检测堆栈（重启才能真正生效）", "check_stack_trace", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(TSPreferenceHelper.createButton(isPurgeEnabled ? "捏捏尾巴" : "检测模块", String.valueOf(Process.myPid()), true, v ->
                new TraceChecker(preferenceLayout).checkAll()));
        TraceChecker.sChildCount = preferenceLayout.getChildCount();
        return preferenceLayout;
    }
}
