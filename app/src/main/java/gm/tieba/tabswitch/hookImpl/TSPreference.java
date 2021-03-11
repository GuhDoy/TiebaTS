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
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BuildConfig;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.util.DisplayHelper;

public class TSPreference extends Hook {
    private static boolean isShowTSPreference = false;
    private static int count = 0;

    public static void hook(ClassLoader classLoader) throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.LogoActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (activity.getIntent().getBooleanExtra("showTSPreference", false))
                    isShowTSPreference = true;
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (isShowTSPreference && !activity.getClass().getName().equals("com.baidu.tieba.LogoActivity")
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

    @SuppressLint({"ApplySharedPref", "ClickableViewAccessibility"})
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
        TSPreferenceHelper.PreferenceLinearLayout preferenceLinearLayout = new TSPreferenceHelper.PreferenceLinearLayout(activity);
        if (tsConfig.getBoolean("ze", false))
            preferenceLinearLayout.addView(TSPreferenceHelper.generateTextView(activity, "轻车简从"));
        else preferenceLinearLayout.addView(TSPreferenceHelper.generateTextView(activity, "净化界面"));
        LinearLayout modifyTab = TSPreferenceHelper.generateButton(classLoader, activity, "修改底栏", null);
        modifyTab.setOnClickListener(v -> showModifyTabDialog(classLoader, activity));
        TSPreferenceHelper.SwitchViewHolder purify = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "真正的净化界面", "purify");
        TSPreferenceHelper.SwitchViewHolder purifyEnter = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "净化进吧", "purify_enter");
        TSPreferenceHelper.SwitchViewHolder purifyMy = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "净化我的", "purify_my");
        TSPreferenceHelper.SwitchViewHolder redTip = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏小红点", "red_tip");
        TSPreferenceHelper.SwitchViewHolder followFilter = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "只推荐已关注的吧", "follow_filter");
        TSPreferenceHelper.SwitchViewHolder personalizedFilter = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "过滤首页推荐", "personalized_filter");
        personalizedFilter.newSwitch.setOnClickListener(v -> showRegexDialog(activity, personalizedFilter, "过滤首页推荐", "personalized_filter"));
        personalizedFilter.bdSwitchView.setOnTouchListener((v, event) -> false);
        TSPreferenceHelper.SwitchViewHolder contentFilter = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "过滤帖子回复", "content_filter");
        contentFilter.newSwitch.setOnClickListener(v -> showRegexDialog(activity, contentFilter, "过滤帖子回复", "content_filter"));
        contentFilter.bdSwitchView.setOnTouchListener((v, event) -> false);
        preferenceLinearLayout.addView(modifyTab);
        if (tsConfig.getBoolean("ze", false)) preferenceLinearLayout.addView(purify);
        preferenceLinearLayout.addView(purifyEnter);
        preferenceLinearLayout.addView(purifyMy);
        preferenceLinearLayout.addView(redTip);
        preferenceLinearLayout.addView(followFilter);
        preferenceLinearLayout.addView(personalizedFilter);
        preferenceLinearLayout.addView(contentFilter);
        if (tsConfig.getBoolean("ze", false))
            preferenceLinearLayout.addView(TSPreferenceHelper.generateTextView(activity, "别出新意"));
        else preferenceLinearLayout.addView(TSPreferenceHelper.generateTextView(activity, "增加功能"));
        TSPreferenceHelper.SwitchViewHolder createView = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "进吧增加收藏、历史", "create_view");
        TSPreferenceHelper.SwitchViewHolder saveImages = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "长按下载保存全部图片", "save_images");
        TSPreferenceHelper.SwitchViewHolder threadStore = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "我的收藏增加搜索", "thread_store");
        preferenceLinearLayout.addView(createView);
        preferenceLinearLayout.addView(saveImages);
        preferenceLinearLayout.addView(threadStore);
        if (tsConfig.getBoolean("ze", false))
            preferenceLinearLayout.addView(TSPreferenceHelper.generateTextView(activity, "垂手可得"));
        else preferenceLinearLayout.addView(TSPreferenceHelper.generateTextView(activity, "自动化"));
        TSPreferenceHelper.SwitchViewHolder autoSign = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "自动签到", "auto_sign");
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
            autoSign.bdSwitchView.setOnTouchListener((v, event) -> false);
        }
        TSPreferenceHelper.SwitchViewHolder openSign = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "自动打开一键签到", "open_sign");
        TSPreferenceHelper.SwitchViewHolder cleanDir = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "更新时清理缓存", "clean_dir");
        TSPreferenceHelper.SwitchViewHolder originSrc = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "自动查看原图", "origin_src");
        preferenceLinearLayout.addView(autoSign);
        preferenceLinearLayout.addView(openSign);
        preferenceLinearLayout.addView(cleanDir);
        preferenceLinearLayout.addView(originSrc);
        if (tsConfig.getBoolean("ze", false))
            preferenceLinearLayout.addView(TSPreferenceHelper.generateTextView(activity, "奇怪怪"));
        else preferenceLinearLayout.addView(TSPreferenceHelper.generateTextView(activity, "其它"));
        TSPreferenceHelper.SwitchViewHolder storageRedirect = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "存储重定向", "storage_redirect");
        TSPreferenceHelper.SwitchViewHolder fontSize = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "禁用帖子缩放手势", "font_size");
        TSPreferenceHelper.SwitchViewHolder eyeshieldMode = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "用夜间模式代替深色模式", "eyeshield_mode");
        preferenceLinearLayout.addView(storageRedirect);
        preferenceLinearLayout.addView(fontSize);
        preferenceLinearLayout.addView(eyeshieldMode);
        if (tsConfig.getBoolean("ze", false))
            preferenceLinearLayout.addView(TSPreferenceHelper.generateTextView(activity, "关于就是关于"));
        else preferenceLinearLayout.addView(TSPreferenceHelper.generateTextView(activity, "关于"));
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
        preferenceLinearLayout.addView(version);
        preferenceLinearLayout.addView(github);
        preferenceLinearLayout.addView(telegram);
        preferenceLinearLayout.addView(author);
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(preferenceLinearLayout.linearLayout);
        AlertDialog alertDialog;
        if (DisplayHelper.isLightMode(activity)) {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle("贴吧TS设置").setView(scrollView).setCancelable(true)
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存并重启", (dialogInterface, i) -> {
                    }).create();
        } else {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                    .setTitle("贴吧TS设置").setView(scrollView).setCancelable(true)
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存并重启", (dialogInterface, i) -> {
                    }).create();
        }
        alertDialog.show();
        isShowTSPreference = false;
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = tsPreference.edit();
            for (int i = 0; i < preferenceLinearLayout.switches.size(); i++) {
                TSPreferenceHelper.SwitchViewHolder switchViewHolder = preferenceLinearLayout.switches.get(i);
                if (Objects.equals("boolean", switchViewHolder.newSwitch.getTag()))
                    editor.putBoolean(switchViewHolder.key, switchViewHolder.isOn());
            }
            editor.commit();
            if (AntiConfusionHelper.getLostList().size() != 0 || AntiConfusionHelper.isDexChanged(activity)) {
                Intent intent = new Intent();
                intent.setClassName(activity, "com.baidu.tieba.launcherGuide.tblauncher.GuideActivity");
                activity.startActivity(intent);
            } else {
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivity(intent);
                activity.finishAffinity();
                System.exit(0);
            }
            alertDialog.dismiss();
        });
    }

    @SuppressLint("ApplySharedPref")
    private static void showModifyTabDialog(ClassLoader classLoader, Activity activity) {
        SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
        TSPreferenceHelper.SwitchViewHolder homeRecommend = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏首页", "home_recommend");
        TSPreferenceHelper.SwitchViewHolder enterForum = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏进吧", "enter_forum");
        TSPreferenceHelper.SwitchViewHolder newCategory = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏频道", "new_category");
        TSPreferenceHelper.SwitchViewHolder myMessage = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏消息", "my_message");
        TSPreferenceHelper.SwitchViewHolder mine = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏我的", "mine");
        TSPreferenceHelper.PreferenceLinearLayout preferenceLinearLayout = new TSPreferenceHelper.PreferenceLinearLayout(activity);
        preferenceLinearLayout.addView(homeRecommend);
        preferenceLinearLayout.addView(enterForum);
        preferenceLinearLayout.addView(newCategory);
        preferenceLinearLayout.addView(myMessage);
        preferenceLinearLayout.addView(mine);
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(preferenceLinearLayout.linearLayout);
        AlertDialog alertDialog;
        if (DisplayHelper.isLightMode(activity)) {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle("修改底栏").setView(scrollView).setCancelable(true)
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存", (dialogInterface, i) -> {
                    }).create();
        } else {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
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
            for (int i = 0; i < preferenceLinearLayout.switches.size(); i++) {
                TSPreferenceHelper.SwitchViewHolder switchViewHolder = preferenceLinearLayout.switches.get(i);
                if (Objects.equals("boolean", switchViewHolder.newSwitch.getTag()))
                    editor.putBoolean(switchViewHolder.key, switchViewHolder.isOn());
            }
            editor.commit();
            alertDialog.dismiss();
        });
    }

    private static final Map<String, String> regex = new HashMap<>();

    @SuppressLint("ApplySharedPref")
    private static void showRegexDialog(Activity activity, TSPreferenceHelper.SwitchViewHolder holder, String title, String key) {
        SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
        EditText editText = new EditText(activity);
        editText.setHint("请输入正则表达式，如.*");
        if (regex.get(key) == null) editText.setText(tsPreference.getString(key, null));
        else editText.setText(regex.get(key));
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setTextSize(18);
        editText.requestFocus();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                regex.put(key, s.toString());
            }
        });
        AlertDialog alertDialog;
        if (DisplayHelper.isLightMode(activity)) {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle(title).setView(editText).setCancelable(true)
                    .setNeutralButton("|", (dialogInterface, i) -> {
                    }).setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存", (dialogInterface, i) -> {
                    }).create();
        } else {
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                    .setTitle(title).setView(editText).setCancelable(true)
                    .setNeutralButton("|", (dialogInterface, i) -> {
                    }).setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存", (dialogInterface, i) -> {
                    }).create();
            editText.setTextColor(Hook.modRes.getColor(R.color.colorPrimary, null));
            editText.setHintTextColor(Hook.modRes.getColor(R.color.colorPrimary, null));
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