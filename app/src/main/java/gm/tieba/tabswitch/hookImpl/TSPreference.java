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
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                parent.addView(TSPreferenceHelper.generateButton(classLoader, activity, "贴吧TS设置", null, v -> showTSPreferenceDialog(classLoader, activity)), 11);
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
        TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        if (tsConfig.getBoolean("ze", false))
            preferenceLayout.addView(TSPreferenceHelper.generateTextView(activity, "轻车简从"));
        else preferenceLayout.addView(TSPreferenceHelper.generateTextView(activity, "净化界面"));
        preferenceLayout.addView(TSPreferenceHelper.generateButton(classLoader, activity, "修改底栏", null, v -> showModifyTabDialog(classLoader, activity)));
        if (tsConfig.getBoolean("ze", false))
            preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "真正的净化界面", "purify"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "净化进吧", "purify_enter"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "净化我的", "purify_my"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏小红点", "red_tip"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "只推荐已关注的吧", "follow_filter"));
        TSPreferenceHelper.SwitchViewHolder personalizedFilter = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "过滤首页推荐", "personalized_filter");
        personalizedFilter.newSwitch.setOnClickListener(v -> showRegexDialog(personalizedFilter));
        personalizedFilter.bdSwitch.setOnTouchListener((v, event) -> false);
        preferenceLayout.addView(personalizedFilter);
        TSPreferenceHelper.SwitchViewHolder contentFilter = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "过滤帖子回复", "content_filter");
        contentFilter.newSwitch.setOnClickListener(v -> showRegexDialog(contentFilter));
        contentFilter.bdSwitch.setOnTouchListener((v, event) -> false);
        preferenceLayout.addView(contentFilter);
        if (tsConfig.getBoolean("ze", false))
            preferenceLayout.addView(TSPreferenceHelper.generateTextView(activity, "别出新意"));
        else preferenceLayout.addView(TSPreferenceHelper.generateTextView(activity, "增加功能"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "进吧增加收藏、历史", "create_view"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "我的收藏增加搜索、吧名", "thread_store"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "浏览历史增加搜索", "history_cache"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "楼层回复增加查看主题贴", "new_sub"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "长按下载保存全部图片", "save_images"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "长按关注的人设置备注名", "my_attention"));
        if (tsConfig.getBoolean("ze", false))
            preferenceLayout.addView(TSPreferenceHelper.generateTextView(activity, "垂手可得"));
        else preferenceLayout.addView(TSPreferenceHelper.generateTextView(activity, "自动化"));
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
            autoSign.bdSwitch.setOnTouchListener((v, event) -> false);
        }
        preferenceLayout.addView(autoSign);
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "自动打开一键签到", "open_sign"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "更新时清理缓存", "clean_dir"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "自动查看原图", "origin_src"));
        if (tsConfig.getBoolean("ze", false))
            preferenceLayout.addView(TSPreferenceHelper.generateTextView(activity, "奇怪怪"));
        else preferenceLayout.addView(TSPreferenceHelper.generateTextView(activity, "其它"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "存储重定向", "storage_redirect"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "禁用帖子缩放手势", "font_size"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "用夜间模式代替深色模式", "eyeshield_mode"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "用赞踩差数代替赞数", "agree_num"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "交换吧热门与最新", "frs_tab"));
        if (tsConfig.getBoolean("ze", false))
            preferenceLayout.addView(TSPreferenceHelper.generateTextView(activity, "关于就是关于"));
        else preferenceLayout.addView(TSPreferenceHelper.generateTextView(activity, "关于"));
        preferenceLayout.addView(TSPreferenceHelper.generateButton(classLoader, activity, "版本", BuildConfig.VERSION_NAME, v -> {
            Intent intentToResolve = TSPreferenceHelper.launchModuleIntent(activity);
            if (intentToResolve == null) return;
            Intent intent = new Intent(intentToResolve);
            List<ResolveInfo> ris = activity.getPackageManager().queryIntentActivities(intentToResolve, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
            activity.startActivity(intent);
        }));
        preferenceLayout.addView(TSPreferenceHelper.generateButton(classLoader, activity, "源代码", "想要小星星", v -> {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://github.com/GuhDoy/TiebaTS"));
            activity.startActivity(intent);
        }));
        preferenceLayout.addView(TSPreferenceHelper.generateButton(classLoader, activity, "TG群", "及时获取更新", v -> {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://t.me/TabSwitch"));
            activity.startActivity(intent);
        }));
        preferenceLayout.addView(TSPreferenceHelper.generateButton(classLoader, activity, "作者", "developed by GM", v -> {
            count++;
            if (count % 3 == 0)
                Toast.makeText(activity, TSPreferenceHelper.randomToast(), Toast.LENGTH_SHORT).show();
            if (count >= 10) {
                SharedPreferences.Editor editor = tsConfig.edit();
                editor.putBoolean("ze", true);
                editor.apply();
            }
        }));
        AlertDialog alertDialog;
        if (DisplayHelper.isLightMode(activity))
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle("贴吧TS设置").setView(preferenceLayout.getPreferenceLayout()).setCancelable(true)
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存并重启", (dialogInterface, i) -> {
                    }).create();
        else alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                .setTitle("贴吧TS设置").setView(preferenceLayout.getPreferenceLayout()).setCancelable(true)
                .setNegativeButton("取消", (dialogInterface, i) -> {
                }).setPositiveButton("保存并重启", (dialogInterface, i) -> {
                }).create();
        alertDialog.show();
        isShowTSPreference = false;
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            SharedPreferences tsPreference = activity.getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = tsPreference.edit();
            for (int i = 0; i < preferenceLayout.switches.size(); i++) {
                TSPreferenceHelper.SwitchViewHolder switchViewHolder = preferenceLayout.switches.get(i);
                if (!switchViewHolder.text.startsWith("过滤"))
                    editor.putBoolean(switchViewHolder.key, switchViewHolder.isOn());
            }
            editor.commit();
            if (AntiConfusionHelper.getLostList().size() != 0) {
                Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.launcherGuide.tblauncher.GuideActivity");
                activity.startActivity(intent);
            } else {
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
        TSPreferenceHelper.PreferenceLayout preferenceLayout = new TSPreferenceHelper.PreferenceLayout(activity);
        TSPreferenceHelper.SwitchViewHolder homeRecommend = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏首页", "home_recommend");
        preferenceLayout.addView(homeRecommend);
        TSPreferenceHelper.SwitchViewHolder enterForum = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏进吧", "enter_forum");
        preferenceLayout.addView(enterForum);
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏频道", "new_category"));
        preferenceLayout.addView(new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏消息", "my_message"));
        TSPreferenceHelper.SwitchViewHolder mine = new TSPreferenceHelper.SwitchViewHolder(classLoader, activity, "隐藏我的", "mine");
        preferenceLayout.addView(mine);
        AlertDialog alertDialog;
        if (DisplayHelper.isLightMode(activity))
            alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle("修改底栏").setView(preferenceLayout.getPreferenceLayout()).setCancelable(true)
                    .setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存", (dialogInterface, i) -> {
                    }).create();
        else alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                .setTitle("修改底栏").setView(preferenceLayout.getPreferenceLayout()).setCancelable(true)
                .setNegativeButton("取消", (dialogInterface, i) -> {
                }).setPositiveButton("保存", (dialogInterface, i) -> {
                }).create();
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (homeRecommend.isOn() && enterForum.isOn()) {
                Toast.makeText(activity, "不能同时隐藏首页和隐藏进吧", Toast.LENGTH_LONG).show();
                return;
            }
            if (mine.isOn())
                Toast.makeText(activity, "隐藏我的后就只能从模块中打开设置了哦", Toast.LENGTH_LONG).show();
            SharedPreferences.Editor editor = tsPreference.edit();
            for (int i = 0; i < preferenceLayout.switches.size(); i++) {
                TSPreferenceHelper.SwitchViewHolder switchViewHolder = preferenceLayout.switches.get(i);
                if (!switchViewHolder.text.startsWith("过滤"))
                    editor.putBoolean(switchViewHolder.key, switchViewHolder.isOn());
            }
            editor.commit();
            alertDialog.dismiss();
        });
    }

    private static final Map<String, String> regex = new HashMap<>();

    @SuppressLint("ApplySharedPref")
    private static void showRegexDialog(TSPreferenceHelper.SwitchViewHolder holder) {
        SharedPreferences tsPreference = holder.newSwitch.getContext().getSharedPreferences("TS_preference", Context.MODE_PRIVATE);
        EditText editText = new EditText(holder.newSwitch.getContext());
        editText.setHint("请输入正则表达式，如.*");
        if (regex.get(holder.key) == null)
            editText.setText(tsPreference.getString(holder.key, null));
        else editText.setText(regex.get(holder.key));
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setTextSize(18);
        editText.requestFocus();
        editText.setHintTextColor(Hook.modRes.getColor(R.color.colorProgress, null));
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                regex.put(holder.key, s.toString());
            }
        });
        AlertDialog alertDialog;
        if (DisplayHelper.isLightMode(holder.newSwitch.getContext()))
            alertDialog = new AlertDialog.Builder(holder.newSwitch.getContext(), AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle(holder.text).setView(editText).setCancelable(true)
                    .setNeutralButton("|", (dialogInterface, i) -> {
                    }).setNegativeButton("取消", (dialogInterface, i) -> {
                    }).setPositiveButton("保存", (dialogInterface, i) -> {
                    }).create();
        else {
            editText.setTextColor(modRes.getColor(R.color.colorPrimary, null));
            alertDialog = new AlertDialog.Builder(holder.newSwitch.getContext(), AlertDialog.THEME_HOLO_DARK)
                    .setTitle(holder.text).setView(editText).setCancelable(true)
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
                if (TextUtils.isEmpty(editText.getText())) {
                    editor.putString(holder.key, null);
                    holder.turnOff();
                } else {
                    Pattern.compile(editText.getText().toString());
                    editor.putString(holder.key, editText.getText().toString());
                    holder.turnOn();
                }
                editor.commit();
                alertDialog.dismiss();
            } catch (PatternSyntaxException e) {
                Toast.makeText(holder.newSwitch.getContext(), Log.getStackTraceString(e), Toast.LENGTH_SHORT).show();
            }
        });
    }
}