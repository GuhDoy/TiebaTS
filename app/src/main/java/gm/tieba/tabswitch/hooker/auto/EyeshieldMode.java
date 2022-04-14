package gm.tieba.tabswitch.hooker.auto;

import static gm.tieba.tabswitch.util.ReflectUtils.getR;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.hooker.anticonfusion.AntiConfusionHelper;
import gm.tieba.tabswitch.util.CollectionsKt;
import gm.tieba.tabswitch.util.DisplayUtils;
import gm.tieba.tabswitch.util.ReflectUtils;

public class EyeshieldMode extends XposedContext implements IHooker {
    private static boolean sSavedUiMode;

    public void hook() throws Throwable {
        sSavedUiMode = DisplayUtils.isLightMode(getContext());
        SharedPreferences.Editor editor = getContext().getSharedPreferences("common_settings",
                Context.MODE_PRIVATE).edit();
        editor.putString("skin_", sSavedUiMode ? "0" : "1");
        editor.apply();
        if (!sSavedUiMode && !AntiConfusionHelper.isVersionChanged(getContext())) {
            XposedHelpers.findAndHookMethod("com.baidu.tieba.LogoActivity", sClassLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    var activity = (Activity) param.thisObject;
                    var nightSplashCache = new File(activity.getFilesDir(), "pic_splash_logo");
                    var nightColor = ReflectUtils.getColor("CAM_X0207");
                    var splashLogo = BitmapFactory.decodeFile(nightSplashCache.getPath());
                    if (splashLogo == null) {
                        var whitePixelColor = getContext().getColor(getR("color", "CAM_X0204"));
                        splashLogo = createNightSplash(BitmapFactory.decodeResource(activity.getResources(),
                                ReflectUtils.getDrawableId("pic_splash_logo")), nightColor, whitePixelColor);
                        try (var out = new FileOutputStream(nightSplashCache)) {
                            splashLogo.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (IOException e) {
                            XposedBridge.log(e);
                        }
                    }

                    var metrics = activity.getResources().getDisplayMetrics();
                    var screenHeight = DisplayUtils.pxToDip(activity, metrics.heightPixels);
                    var lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, screenHeight);
                    lp.gravity = Gravity.BOTTOM | Gravity.CENTER;
                    var root = new FrameLayout(activity);
                    root.setLayoutParams(lp);

                    var logoView = new ImageView(activity);
                    logoView.setImageBitmap(splashLogo);
                    root.addView(logoView);

                    var decorView = (ViewGroup) activity.getWindow().getDecorView();
                    decorView.setBackgroundColor(nightColor);
                    decorView.addView(root);
                }

                @NonNull
                private Bitmap createNightSplash(Bitmap immutable, int nightColor, int whitePixelColor) {
                    // create a mutable bitmap
                    var splashLogo = immutable.copy(immutable.getConfig(), true);
                    immutable.recycle();
                    // alter its color
                    var width = splashLogo.getWidth();
                    var height = splashLogo.getHeight();
                    var pixels = new int[width * height];
                    splashLogo.getPixels(pixels, 0, width, 0, 0, width, height);
                    var candidates = new ArrayList<Integer>();
                    for (var i = 0; i < pixels.length; i++) {
                        if (pixels[i] == -1) {
                            pixels[i] = nightColor;
                        } else {
                            candidates.add(pixels[i]);
                        }
                    }
                    var blackPixelColor = CollectionsKt.most(candidates);
                    for (var i = 0; i < pixels.length; i++) {
                        if (pixels[i] == blackPixelColor) {
                            pixels[i] = whitePixelColor;
                        }
                    }
                    splashLogo.setPixels(pixels, 0, width, 0, 0, width, height);
                    return splashLogo;
                }
            });
        }
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", sClassLoader, "onConfigurationChanged", Configuration.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var activity = (Activity) param.thisObject;
                if (sSavedUiMode != DisplayUtils.isLightMode(activity)) {
                    sSavedUiMode = DisplayUtils.isLightMode(activity);

                    // com.baidu.tieba.setting.more.MoreActivity.OnSwitchStateChange()
                    if (!DisplayUtils.isLightMode(activity)) {
                        // CAM_X0201_1
                        var color = ReflectUtils.getColor("CAM_X0201");
                        XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("com.baidu.tbadk.core.util.UtilHelper", sClassLoader),
                                "setNavigationBarBackground", activity, color
                        );

                        XposedHelpers.callMethod(activity, "onChangeSkinType", 1);
                        var app = XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("com.baidu.tbadk.core.TbadkCoreApplication", sClassLoader),
                                "getInst"
                        );
                        XposedHelpers.callMethod(app, "setSkinType", 1);
                    } else {
                        XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("com.baidu.tbadk.core.util.SkinManager", sClassLoader),
                                "setDayOrDarkSkinTypeWithSystemMode", true, false
                        );
                    }
                }
            }
        });
    }
}
