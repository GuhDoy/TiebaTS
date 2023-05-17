package gm.tieba.tabswitch.hooker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
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

import java.io.File;
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
import gm.tieba.tabswitch.hooker.add.MyAttention;
import gm.tieba.tabswitch.hooker.deobfuscation.DeobfuscationHelper;
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;
import gm.tieba.tabswitch.hooker.deobfuscation.StringMatcher;
import gm.tieba.tabswitch.hooker.extra.TraceChecker;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.widget.NavigationBar;
import gm.tieba.tabswitch.widget.TbDialog;
import gm.tieba.tabswitch.widget.TbToast;

public class TSPreference extends XposedContext implements IHooker, Obfuscated {
    public final static String MAIN = "贴吧TS设置";
    public final static String MODIFY_TAB = "修改页面";
    public final static String NOTES = "备注关注的人";
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
        return List.of(new StringMatcher("reply_private_setting_switch"));
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
                                case NOTES:
                                    proxyPage(activity, navigationBar, NOTES, MyAttention.createNotesPreference(activity));
                                    break;
                                case TRACE:
                                    proxyPage(activity, navigationBar, TRACE, createHidePreference(activity));
                                    break;
                            }
                        } catch (final Throwable tr) {
                            final var messages = new ArrayList<String>();
                            messages.add(Constants.getStrings().get("exception_init_preference"));
                            messages.add(String.format(Locale.CHINA, "tbversion: %s, module version: %d",
                                    DeobfuscationHelper.getTbVersion(getContext()), BuildConfig.VERSION_CODE));
                            messages.add(Log.getStackTraceString(tr));
                            final var message = TextUtils.join("\n", messages);
                            XposedBridge.log(message);
                            new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                    .setTitle("警告").setMessage(message).setCancelable(false)
                                    .setPositiveButton(activity.getString(android.R.string.ok), (dialogInterface, i) -> activity.finish())
                                    .show();
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
        navigationBar.addTextButton("重启", v -> DisplayUtils.restart(activity));
        final var contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        final var parent = (LinearLayout) contentView.getChildAt(0);
        final var mainScroll = (ScrollView) parent.getChildAt(1);
        final var containerView = (LinearLayout) mainScroll.getChildAt(0);
        containerView.removeAllViews();
        containerView.addView(preferenceLayout);
    }

    private void startRootPreferenceActivity(final Activity activity) {
        if (!Preferences.getIsEULAAccepted()) {
            final StringBuilder stringBuilder = new StringBuilder().append(Constants.getStrings().get("EULA"));
            if (BuildConfig.VERSION_NAME.contains("alpha") || BuildConfig.VERSION_NAME.contains("beta")) {
                stringBuilder.append("\n\n").append(Constants.getStrings().get("dev_tip"));
            }
            final TbDialog bdAlert = new TbDialog(activity, "使用协议", stringBuilder.toString(), true, null);
            bdAlert.setOnNoButtonClickListener(v -> {
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + (sPath.contains(BuildConfig.APPLICATION_ID)
                        && new File(sPath).exists() ?
                        BuildConfig.APPLICATION_ID : activity.getPackageName())));
                activity.startActivity(intent);
            });
            bdAlert.setOnYesButtonClickListener(v -> {
                Preferences.putEULAAccepted();
                startRootPreferenceActivity(activity);
                bdAlert.dismiss();
            });
            bdAlert.show();
        } else {
            final Intent intent = new Intent().setClassName(activity, PROXY_ACTIVITY);
            intent.putExtra("proxyPage", MAIN);
            activity.startActivity(intent);
        }
    }

    @NotNull
    private LinearLayout createRootPreference(final Activity activity) {
        final boolean isPurgeEnabled = Preferences.getIsPurgeEnabled();
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
//        preferenceLayout.addView(new SwitchButtonHolder(activity, "净化我的", "purge_my", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏小红点", "red_tip", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "禁用更新提示", "remove_update", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "只推荐已关注的吧", "follow_filter", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "过滤首页推荐", "personalized_filter", SwitchButtonHolder.TYPE_DIALOG));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "过滤帖子回复", "content_filter", SwitchButtonHolder.TYPE_DIALOG));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "过滤吧页面", "frs_page_filter", SwitchButtonHolder.TYPE_DIALOG));

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurgeEnabled ? "别出新意" : "增加功能"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "进吧增加收藏、历史", "create_view", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "我的收藏增加搜索、吧名", "thread_store", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "浏览历史增加搜索", "history_cache", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "搜索楼中楼增加查看主题贴", "new_sub", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "楼层增加点按效果", "ripple", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "长按下载保存全部图片", "save_images", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(TSPreferenceHelper.createButton(NOTES, null, true, v -> {
            final Intent intent = new Intent().setClassName(activity, PROXY_ACTIVITY);
            intent.putExtra("proxyPage", NOTES);
            activity.startActivity(intent);
        }));

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurgeEnabled ? "垂手可得" : "自动化"));
        final SwitchButtonHolder autoSign = new SwitchButtonHolder(activity, "自动签到", "auto_sign", SwitchButtonHolder.TYPE_SWITCH);
        if (!Preferences.getIsAutoSignEnabled()) {
            autoSign.setOnButtonClickListener(v -> {
                final TbDialog bdalert = new TbDialog(activity, "提示",
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
        preferenceLayout.addView(new SwitchButtonHolder(activity, "吧页面起始页面改为最新", "frs_tab", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "自动查看原图", "origin_src", SwitchButtonHolder.TYPE_SWITCH));

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurgeEnabled ? "奇怪怪" : "其它"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "使用媒体存储保存图片", "redirect_image", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "禁用帖子手势", "forbid_gesture", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "用赞踩差数代替赞数", "agree_num", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(TSPreferenceHelper.createButton(TRACE, "希望有一天不再需要贴吧TS", true, v -> {
            final Intent intent = new Intent().setClassName(activity, PROXY_ACTIVITY);
            intent.putExtra("proxyPage", TRACE);
            activity.startActivity(intent);
        }));

        preferenceLayout.addView(TSPreferenceHelper.createTextView(isPurgeEnabled ? "关于就是关于" : "关于"));
        preferenceLayout.addView(TSPreferenceHelper.createButton("版本", BuildConfig.VERSION_NAME, true, v -> {
            final Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://github.com/GuhDoy/TiebaTS/releases/latest"));
            activity.startActivity(intent);
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
        return preferenceLayout;
    }

    private LinearLayout createModifyTabPreference(final Activity activity) {
        final TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        preferenceLayout.addView(TSPreferenceHelper.createTextView("主页导航栏"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏首页", "home_recommend", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏进吧", "enter_forum", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏发帖", "write_thread", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "隐藏消息", "im_message", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "禁用动态样式", "dynamic_style", SwitchButtonHolder.TYPE_SWITCH));
        preferenceLayout.addView(TSPreferenceHelper.createTextView("禁用 Flutter"));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "我关注的吧", "flutter_concern_forum_enable_android", SwitchButtonHolder.TYPE_SET_FLUTTER));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "吧资料", "flutter_forum_detail_enable_android_112", SwitchButtonHolder.TYPE_SET_FLUTTER));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "粉丝", "flutter_attention_enable_android_112", SwitchButtonHolder.TYPE_SET_FLUTTER));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "个人中心", "flutter_person_center_enable_android_12", SwitchButtonHolder.TYPE_SET_FLUTTER));
        preferenceLayout.addView(new SwitchButtonHolder(activity, "一键签到", "flutter_signin_enable_android_119", SwitchButtonHolder.TYPE_SET_FLUTTER));
        return preferenceLayout;
    }

    private LinearLayout createHidePreference(final Activity activity) {
        final boolean isPurgeEnabled = Preferences.getIsPurgeEnabled();
        final TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        if (isPurgeEnabled || BuildConfig.DEBUG) {
            preferenceLayout.addView(TSPreferenceHelper.createTextView(null));
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
