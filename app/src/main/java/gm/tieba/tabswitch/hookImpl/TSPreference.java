package gm.tieba.tabswitch.hookImpl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import gm.tieba.tabswitch.BuildConfig;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.R;

public class TSPreference extends Hook {
    private static boolean isShowDialog = false;
    private static int count = 0;

    public static void hook(ClassLoader classLoader) throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.LogoActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (activity.getIntent().getBooleanExtra("openTSPreference", false))
                    isShowDialog = true;
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (isShowDialog && !activity.getClass().getName().equals("com.baidu.tieba.LogoActivity")
                        && !activity.getClass().getName().equals("com.baidu.tieba.launcherGuide.tblauncher.GuideActivity"))
                    showTSPreferenceDialog(classLoader, activity);
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tieba.setting.more.MoreActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                FrameLayout browseSetting = activity.findViewById(classLoader.loadClass("com.baidu.tieba.R$id").getField("browseSetting").getInt(null));
                LinearLayout parent = (LinearLayout) browseSetting.getParent();
                LinearLayout TSPreferenceButton = TSPreferenceHelper.generateButton(classLoader, activity, "贴吧TS设置", null);
                parent.addView(TSPreferenceButton, 11);
                TSPreferenceButton.setOnClickListener(v -> showTSPreferenceDialog(classLoader, activity));
            }
        });
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
    }

    @SuppressLint("ApplySharedPref")
    private static void showTSPreferenceDialog(ClassLoader classLoader, Activity activity) {
        SharedPreferences tsConfig = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
        if (!tsConfig.getBoolean("EULA", false)) {
            StringBuilder stringBuilder = new StringBuilder().append("本模块开源免费，不会主动发起网络请求，不会上传任何用户数据，旨在技术交流。请勿将本模块用于商业或非法用途，由此产生的后果与开发者无关。\n若您不同意此协议，请立即卸载本模块！无论您以何种形式或方式使用本模块，皆视为您已同意此协议！");
            if (BuildConfig.VERSION_NAME.contains("alpha") || BuildConfig.VERSION_NAME.contains("beta"))
                stringBuilder.append("\n\n提示：您当前安装的是非正式版本，可能含有较多错误，如果您希望得到更稳定的使用体验，建议您安装正式版本。");
            AlertDialog alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle("使用协议").setMessage(stringBuilder.toString()).setCancelable(true)
                    .setNegativeButton("拒绝", (dialogInterface, i) -> {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_DELETE);
                        Intent intentToResolve = TSPreferenceHelper.launchModuleIntent(activity);
                        if (intentToResolve != null)
                            intent.setData(Uri.parse("package:" + "gm.tieba.tabswitch"));
                        else intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    }).setPositiveButton("同意", (dialogInterface, i) -> {
                        SharedPreferences.Editor editor = tsConfig.edit();
                        editor.putBoolean("EULA", true);
                        editor.apply();
                        showTSPreferenceDialog(classLoader, activity);
                    }).create();
            alertDialog.show();
            return;
        }
        SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(30, 0, 30, 0);
        if (tsConfig.getBoolean("ze", false))
            linearLayout.addView(TSPreferenceHelper.generateTextView(activity, "轻车简从"));
        else linearLayout.addView(TSPreferenceHelper.generateTextView(activity, "净化界面"));
        LinearLayout modifyTab = TSPreferenceHelper.generateButton(classLoader, activity, "修改底栏", null);
        modifyTab.setOnClickListener(v -> showModifyTabDialog(classLoader, activity));
        TSPreferenceHelper.SwitchViewHolder purify = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "真正的净化界面", tsPreference.getBoolean("purify", false));
        TSPreferenceHelper.SwitchViewHolder purifyEnter = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "净化进吧", tsPreference.getBoolean("purify_enter", false));
        TSPreferenceHelper.SwitchViewHolder purifyMy = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "净化我的", tsPreference.getBoolean("purify_my", false));
        TSPreferenceHelper.SwitchViewHolder redTip = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏小红点", tsPreference.getBoolean("red_tip", false));
        TSPreferenceHelper.SwitchViewHolder followFilter = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "只推荐已关注的吧", tsPreference.getBoolean("follow_filter", false));
        TSPreferenceHelper.SwitchViewHolder personalizedFilter = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "过滤首页推荐", tsPreference.getString("personalized_filter", null) != null);
        personalizedFilter.newSwitch.setOnClickListener(v -> showRegexDialog(activity, personalizedFilter, "过滤首页推荐", "personalized_filter"));
        personalizedFilter.switchInstance.setOnTouchListener((v, event) -> false);
        TSPreferenceHelper.SwitchViewHolder contentFilter = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "过滤帖子回复", tsPreference.getString("content_filter", null) != null);
        contentFilter.newSwitch.setOnClickListener(v -> showRegexDialog(activity, contentFilter, "过滤帖子回复", "content_filter"));
        contentFilter.switchInstance.setOnTouchListener((v, event) -> false);
        linearLayout.addView(modifyTab);
        if (tsConfig.getBoolean("ze", false)) linearLayout.addView(purify.newSwitch);
        linearLayout.addView(purifyEnter.newSwitch);
        linearLayout.addView(purifyMy.newSwitch);
        linearLayout.addView(redTip.newSwitch);
        linearLayout.addView(followFilter.newSwitch);
        linearLayout.addView(personalizedFilter.newSwitch);
        linearLayout.addView(contentFilter.newSwitch);
        if (tsConfig.getBoolean("ze", false))
            linearLayout.addView(TSPreferenceHelper.generateTextView(activity, "别出新意"));
        else linearLayout.addView(TSPreferenceHelper.generateTextView(activity, "增加功能"));
        TSPreferenceHelper.SwitchViewHolder createView = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "进吧增加收藏、历史", tsPreference.getBoolean("create_view", false));
        linearLayout.addView(createView.newSwitch);
        if (tsConfig.getBoolean("ze", false))
            linearLayout.addView(TSPreferenceHelper.generateTextView(activity, "垂手可得"));
        else linearLayout.addView(TSPreferenceHelper.generateTextView(activity, "自动化"));
        TSPreferenceHelper.SwitchViewHolder autoSign = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "自动签到", tsPreference.getBoolean("auto_sign", false));
        if (!tsConfig.getBoolean("auto_sign", false)) {
            autoSign.newSwitch.setOnClickListener(v -> {
                AlertDialog alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                        .setTitle("提示").setMessage("这是一个需要网络请求并且有封号风险的功能，您需要自行承担使用此功能的风险，请谨慎使用！").setCancelable(true)
                        .setNegativeButton("取消", (dialogInterface, i) -> {
                        }).setPositiveButton("确定", (dialogInterface, i) -> {
                            SharedPreferences.Editor editor = tsConfig.edit();
                            editor.putBoolean("auto_sign", true);
                            editor.apply();
                            autoSign.turnOn();
                        }).create();
                alertDialog.show();
            });
            autoSign.switchInstance.setOnTouchListener((v, event) -> false);
        }
        TSPreferenceHelper.SwitchViewHolder openSign = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "自动打开一键签到", tsPreference.getBoolean("open_sign", false));
        TSPreferenceHelper.SwitchViewHolder cleanDir = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "更新时清理缓存", tsPreference.getBoolean("clean_dir", false));
        linearLayout.addView(autoSign.newSwitch);
        linearLayout.addView(openSign.newSwitch);
        linearLayout.addView(cleanDir.newSwitch);
        if (tsConfig.getBoolean("ze", false))
            linearLayout.addView(TSPreferenceHelper.generateTextView(activity, "奇怪怪"));
        else linearLayout.addView(TSPreferenceHelper.generateTextView(activity, "其它"));
        TSPreferenceHelper.SwitchViewHolder storageRedirect = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "存储重定向", tsPreference.getBoolean("storage_redirect", false));
        TSPreferenceHelper.SwitchViewHolder fontSize = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "禁用帖子缩放手势", tsPreference.getBoolean("font_size", false));
        TSPreferenceHelper.SwitchViewHolder eyeshieldMode = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "用夜间模式代替深色模式", tsPreference.getBoolean("eyeshield_mode", false));
        TSPreferenceHelper.SwitchViewHolder personalizedFilterLog = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "打印过滤首页推荐日志", tsPreference.getBoolean("personalized_filter_log", false));
        linearLayout.addView(storageRedirect.newSwitch);
        linearLayout.addView(fontSize.newSwitch);
        linearLayout.addView(eyeshieldMode.newSwitch);
        linearLayout.addView(personalizedFilterLog.newSwitch);
        if (tsConfig.getBoolean("ze", false))
            linearLayout.addView(TSPreferenceHelper.generateTextView(activity, "关于就是关于"));
        else linearLayout.addView(TSPreferenceHelper.generateTextView(activity, "关于"));
        LinearLayout version = TSPreferenceHelper.generateButton(classLoader, activity, "版本", BuildConfig.VERSION_NAME);
        version.setOnClickListener(v -> {
            Intent intentToResolve = TSPreferenceHelper.launchModuleIntent(activity);
            if (intentToResolve == null) return;
            Intent intent = new Intent(intentToResolve);
            List<ResolveInfo> ris = activity.getPackageManager().queryIntentActivities(intentToResolve, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
            activity.startActivity(intent);
        });
        LinearLayout github = TSPreferenceHelper.generateButton(classLoader, activity, "源代码", "想要小星星");
        github.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://github.com/GuhDoy/TiebaTS"));
            activity.startActivity(intent);
        });
        LinearLayout telegram = TSPreferenceHelper.generateButton(classLoader, activity, "TG群", "及时获取更新");
        telegram.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://t.me/TabSwitch"));
            activity.startActivity(intent);
        });
        LinearLayout author = TSPreferenceHelper.generateButton(classLoader, activity, "作者", "developed by GM");
        author.setOnClickListener(v -> {
            count++;
            if (count % 3 == 0)
                Toast.makeText(activity, TSPreferenceHelper.randomToast(), Toast.LENGTH_SHORT).show();
            if (count >= 10) {
                SharedPreferences.Editor editor = tsConfig.edit();
                editor.putBoolean("ze", true);
                editor.apply();
            }
        });
        linearLayout.addView(version);
        linearLayout.addView(github);
        linearLayout.addView(telegram);
        linearLayout.addView(author);
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(linearLayout);
        AlertDialog alertDialog;
        if ((activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                    .setTitle("贴吧TS设置").setView(scrollView).setCancelable(true)
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存并重启", (dialogInterface, i) -> {
                    }).create();
        } else {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle("贴吧TS设置").setView(scrollView).setCancelable(true)
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存并重启", (dialogInterface, i) -> {
                    }).create();
        }
        alertDialog.show();
        isShowDialog = false;
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            SharedPreferences.Editor editor = tsPreference.edit();
            editor.putBoolean("purify", purify.isOn());
            editor.putBoolean("purify_enter", purifyEnter.isOn());
            editor.putBoolean("purify_my", purifyMy.isOn());
            editor.putBoolean("red_tip", redTip.isOn());
            editor.putBoolean("follow_filter", followFilter.isOn());
            editor.putBoolean("create_view", createView.isOn());
            editor.putBoolean("auto_sign", autoSign.isOn());
            editor.putBoolean("open_sign", openSign.isOn());
            editor.putBoolean("clean_dir", cleanDir.isOn());
            editor.putBoolean("storage_redirect", storageRedirect.isOn());
            editor.putBoolean("font_size", fontSize.isOn());
            editor.putBoolean("eyeshield_mode", eyeshieldMode.isOn());
            editor.putBoolean("personalized_filter_log", personalizedFilterLog.isOn());
            editor.commit();
            SharedPreferences sharedPreferences = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
            if (tsConfig.getString("anti-confusion_version", "unknown").equals(sharedPreferences.getString("key_rate_version", "unknown"))
                    && !sharedPreferences.getString("key_rate_version", "unknown").equals("unknown")) {
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                System.exit(0);
            } else {
                Intent intent = new Intent();
                intent.setClassName(activity, "com.baidu.tieba.launcherGuide.tblauncher.GuideActivity");
                activity.startActivity(intent);
            }
        });
    }

    @SuppressLint("ApplySharedPref")
    private static void showModifyTabDialog(ClassLoader classLoader, Activity activity) {
        SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
        TSPreferenceHelper.SwitchViewHolder homeRecommend = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏首页", tsPreference.getBoolean("home_recommend", false));
        TSPreferenceHelper.SwitchViewHolder enterForum = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏进吧", tsPreference.getBoolean("enter_forum", false));
        TSPreferenceHelper.SwitchViewHolder newCategory = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏频道", tsPreference.getBoolean("new_category", false));
        TSPreferenceHelper.SwitchViewHolder myMessage = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏消息", tsPreference.getBoolean("my_message", false));
        TSPreferenceHelper.SwitchViewHolder mine = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏我的", tsPreference.getBoolean("mine", false));
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(50, 0, 50, 0);
        linearLayout.addView(homeRecommend.newSwitch);
        linearLayout.addView(enterForum.newSwitch);
        linearLayout.addView(newCategory.newSwitch);
        linearLayout.addView(myMessage.newSwitch);
        linearLayout.addView(mine.newSwitch);
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(linearLayout);
        AlertDialog alertDialog;
        if ((activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                    .setTitle("修改底栏").setView(scrollView).setCancelable(true)
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存", (dialogInterface, i) -> {
                    }).create();
        } else {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle("修改底栏").setView(scrollView).setCancelable(true)
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存", (dialogInterface, i) -> {
                    }).create();
        }
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (homeRecommend.isOn() && enterForum.isOn()) {
                Toast.makeText(activity, "不能同时隐藏首页和隐藏进吧", Toast.LENGTH_LONG).show();
                return;
            }
            if (mine.isOn())
                Toast.makeText(activity, "隐藏我的后就只能从模块中打开设置了哦", Toast.LENGTH_LONG).show();
            SharedPreferences.Editor editor = tsPreference.edit();
            editor.putBoolean("home_recommend", homeRecommend.isOn());
            editor.putBoolean("enter_forum", enterForum.isOn());
            editor.putBoolean("new_category", newCategory.isOn());
            editor.putBoolean("my_message", myMessage.isOn());
            editor.putBoolean("mine", mine.isOn());
            editor.commit();
            alertDialog.dismiss();
        });
    }

    @SuppressLint("ApplySharedPref")
    private static void showRegexDialog(Activity activity, TSPreferenceHelper.SwitchViewHolder holder, String title, String key) {
        SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
        EditText editText = new EditText(activity);
        editText.setHint("请输入正则表达式，如.*");
        editText.setText(tsPreference.getString(key, null));
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setTextSize(18);
        editText.requestFocus();
        AlertDialog alertDialog;
        if ((activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                    .setTitle(title).setView(editText).setCancelable(true)
                    .setNeutralButton("|", (dialogInterface, i) -> {
                    }).setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存", (dialogInterface, i) -> {
                    }).create();
            editText.setTextColor(Hook.modRes.getColor(R.color.colorPrimary, null));
            editText.setHintTextColor(Hook.modRes.getColor(R.color.colorPrimary, null));
        } else {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle(title).setView(editText).setCancelable(true)
                    .setNeutralButton("|", (dialogInterface, i) -> {
                    }).setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存", (dialogInterface, i) -> {
                    }).create();
        }
        alertDialog.show();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            int selectionStart = editText.getSelectionStart();
            int selectionEnd = editText.getSelectionEnd();
            String sub1 = editText.getText().toString().substring(0, selectionEnd);
            String sub2 = editText.getText().toString().substring(selectionEnd);
            editText.setText(String.format("%s|%s", sub1, sub2));
            if (selectionStart == selectionEnd) editText.setSelection(selectionEnd + 1);
            else editText.setSelection(selectionStart, selectionEnd + 1);
        });
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            SharedPreferences.Editor editor = tsPreference.edit();
            try {
                if ("".equals(editText.getText().toString())) {
                    editor.putString(key, null);
                    holder.turnOff();
                } else {
                    Pattern.compile(editText.getText().toString());
                    editor.putString(key, editText.getText().toString());
                    holder.turnOn();
                }
                editor.commit();
                alertDialog.dismiss();
            } catch (PatternSyntaxException e) {
                Toast.makeText(activity, Log.getStackTraceString(e), Toast.LENGTH_SHORT).show();
            }
        });
    }
}