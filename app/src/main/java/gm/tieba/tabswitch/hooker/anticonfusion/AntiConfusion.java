package gm.tieba.tabswitch.hooker.anticonfusion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.stream.Collectors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.Parser;

public class AntiConfusion extends XposedContext implements IHooker {
    private static final String TRAMPOLINE_ACTIVITY = "com.baidu.tieba.tblauncher.MainTabActivity";
    private final AntiConfusionViewModel viewModel = new AntiConfusionViewModel();
    private Activity mActivity;
    private TextView mMessage;
    private TextView mProgress;
    private RelativeLayout mProgressContainer;
    private LinearLayout mContentView;

    public void hook() throws Throwable {
        try {
            // make isLaunchOpOn = false to avoid startActivity() to com.baidu.tieba.NewLogoActivity
            XposedHelpers.findAndHookMethod("com.baidu.tbadk.switchs.AdToMainTabActivitySwitch", sClassLoader,
                    "getIsOn", XC_MethodReplacement.returnConstant(false));
        } catch (XposedHelpers.ClassNotFoundError ignored) {
        }
//        var unhook = XposedHelpers.findAndHookMethod(Instrumentation.class, "execStartActivity",
//                Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class,
//                int.class, Bundle.class, XC_MethodReplacement.returnConstant(null));
        XposedHelpers.findAndHookMethod("com.baidu.tieba.LogoActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                mActivity = (Activity) param.thisObject;
                if (Preferences.getBoolean("purge")) {
                    var editor = mActivity
                            .getSharedPreferences("settings", Context.MODE_PRIVATE)
                            .edit();
                    editor.putString("key_location_request_dialog_last_show_version",
                            AntiConfusionHelper.getTbVersion(mActivity)
                    );
                    editor.commit();
                }

                if (AntiConfusionHelper.isDexChanged(mActivity)) {
                    AcRules.dropRules();
                } else if (!AntiConfusionHelper.getRulesLost().isEmpty()) {
                    AntiConfusionHelper.matcherList = AntiConfusionHelper.getRulesLost();
                } else {
//                    unhook.unhook();
                    AntiConfusionHelper.saveAndRestart(mActivity,
                            AntiConfusionHelper.getTbVersion(mActivity),
                            XposedHelpers.findClass(TRAMPOLINE_ACTIVITY, sClassLoader)
                    );
                    return;
                }

                initProgressIndicator();
                mActivity.setContentView(mContentView);
                viewModel.progress.subscribe(progress -> setProgress(progress));

                new Thread(() -> {
                    try {
                        setMessage("解压");
                        var packageResource = new File(mActivity.getPackageResourcePath());
                        var dexDir = new File(mActivity.getCacheDir(), "app_dex");
                        viewModel.unzip(packageResource, dexDir);

                        setMessage("搜索字符串和资源 id");
                        var idToMatcher = Parser.resolveIdentifier(AntiConfusionHelper.matcherList);
                        AntiConfusionHelper.matcherList.removeAll(idToMatcher.values());
                        var searcher = new DexBakSearcher(AntiConfusionHelper.matcherList,
                                idToMatcher.keySet().stream()
                                        .map(Long::valueOf)
                                        .collect(Collectors.toList())
                        );
                        var scope = viewModel.fastSearchAndFindScope(searcher, idToMatcher);

                        setMessage("在 " + scope.getMost() + " 中搜索代码");
                        viewModel.searchSmali(searcher, scope);

                        viewModel.saveDexSignatureHashCode();
                        XposedBridge.log("anti-confusion accomplished, current version: "
                                + AntiConfusionHelper.getTbVersion(mActivity));
//                        unhook.unhook();
                        AntiConfusionHelper.saveAndRestart(mActivity,
                                AntiConfusionHelper.getTbVersion(mActivity),
                                XposedHelpers.findClass(TRAMPOLINE_ACTIVITY, sClassLoader)
                        );
                    } catch (Throwable e) {
                        XposedBridge.log(e);
                        setMessage("处理失败\n" + Log.getStackTraceString(e));
                    }
                }).start();
            }
        });
    }

    @SuppressLint({"SetTextI18n"})
    private void initProgressIndicator() {
        var title = new TextView(mActivity);
        title.setTextSize(16);
        title.setPaddingRelative(0, 0, 0, 20);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.parseColor("#FF303030"));
        title.setText("贴吧TS正在定位被混淆的类和方法，请耐心等待");
        mMessage = new TextView(mActivity);
        mMessage.setTextSize(16);
        mMessage.setTextColor(Color.parseColor("#FF303030"));
        mProgress = new TextView(mActivity);
        mProgress.setBackgroundColor(Color.parseColor("#FFBEBEBE"));
        mProgressContainer = new RelativeLayout(mActivity);
        mProgressContainer.addView(mProgress);
        mProgressContainer.addView(mMessage);
        var tvLp = (RelativeLayout.LayoutParams) mMessage.getLayoutParams();
        tvLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mMessage.setLayoutParams(tvLp);
        var rlLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mProgressContainer.setLayoutParams(rlLp);
        mContentView = new LinearLayout(mActivity);
        mContentView.setOrientation(LinearLayout.VERTICAL);
        mContentView.setGravity(Gravity.CENTER);
        mContentView.addView(title);
        mContentView.addView(mProgressContainer);
    }

    private void setMessage(String message) {
        mActivity.runOnUiThread(() -> mMessage.setText(message));
    }

    private void setProgress(float progress) {
        mActivity.runOnUiThread(() -> {
            var lp = mProgress.getLayoutParams();
            lp.height = mMessage.getHeight();
            lp.width = Math.round(mProgressContainer.getWidth() * progress);
            mProgress.setLayoutParams(lp);
        });
    }
}
