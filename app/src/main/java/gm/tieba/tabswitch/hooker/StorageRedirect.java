package gm.tieba.tabswitch.hooker;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.Hooker;
import gm.tieba.tabswitch.hooker.model.Rule;
import gm.tieba.tabswitch.util.IO;

public class StorageRedirect extends BaseHooker implements Hooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod(Environment.class, "getExternalStorageDirectory", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(sContextRef.get().getExternalCacheDir());
            }
        });
        Rule.findRule(new Rule.RuleCallBack() {
            @Override
            public void onRuleFound(String rule, String clazz, String method) throws ClassNotFoundException {
                for (Method md : sClassLoader.loadClass(clazz).getDeclaredMethods())
                    switch (Arrays.toString(md.getParameterTypes())) {
                        case "[class java.lang.String, class [B, class android.content.Context]":
                            XposedBridge.hookMethod(md, new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                    return saveImage((String) param.args[0], new ByteArrayInputStream((byte[]) param.args[1]), (Context) param.args[2]);
                                }
                            });
                            break;
                        case "[class java.lang.String, class java.lang.String, class android.content.Context]":
                            XposedBridge.hookMethod(md, new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                    return saveImage((String) param.args[1], new FileInputStream((String) param.args[0]), (Context) param.args[2]);
                                }
                            });
                            break;
                    }
            }
        }, "0x4197d783fc000000L");
    }

    private int saveImage(String url, InputStream in, Context context) {
        Context applicationContext = context.getApplicationContext();
        String fileName = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
        try {
            ByteArrayOutputStream baos = IO.cloneInputStream(in);
            String extension = IO.getExtension(baos);
            in = new ByteArrayInputStream(baos.toByteArray());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues newImageDetails = new ContentValues();
                newImageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "tieba");
                newImageDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                newImageDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + extension);
                ContentResolver resolver = applicationContext.getContentResolver();
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newImageDetails);
                ParcelFileDescriptor descriptor = resolver.openFileDescriptor(imageUri, "w");
                IO.copyFile(in, descriptor.getFileDescriptor());
            } else {
                File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "tieba");
                imageDir.mkdirs();
                IO.copyFile(in, new File(imageDir.getPath(), fileName + "." + extension));

                Intent scanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
                scanIntent.setData(Uri.fromFile(imageDir));
                applicationContext.sendBroadcast(scanIntent);
            }
            Looper.prepare();
            Toast.makeText(applicationContext, fileName + "." + extension, Toast.LENGTH_SHORT).show();
            Looper.loop();
        } catch (IOException e) {
            XposedBridge.log(e);
            return -1;
        }
        return 0;
    }
}