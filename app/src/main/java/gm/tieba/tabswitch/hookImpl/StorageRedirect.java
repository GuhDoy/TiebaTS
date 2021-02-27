package gm.tieba.tabswitch.hookImpl;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.util.IO;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class StorageRedirect extends Hook {
    public static void hook(ClassLoader classLoader, Context context) throws Throwable {
        XposedHelpers.findAndHookMethod(Environment.class, "getExternalStorageDirectory", new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(context.getExternalCacheDir());
            }
        });
        for (int i = 0; i < ruleMapList.size(); i++) {
            Map<String, String> map = ruleMapList.get(i);
            if (Objects.equals(map.get("rule"), "0x4197d783fc000000L")) {
                Method[] methods = classLoader.loadClass(map.get("class")).getDeclaredMethods();
                for (Method method : methods)
                    switch (Arrays.toString(method.getParameterTypes())) {
                        case "[class java.lang.String, class [B, class android.content.Context]":
                            XposedBridge.hookMethod(method, new XC_MethodReplacement() {
                                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                    return saveImage((String) param.args[0], (Context) param.args[2]);
                                }
                            });
                            break;
                        case "[class java.lang.String, class java.lang.String, class android.content.Context]":
                            XposedBridge.hookMethod(method, new XC_MethodReplacement() {
                                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                    return saveImage((String) param.args[1], (Context) param.args[2]);
                                }
                            });
                            break;
                    }
            }
        }
    }

    private static int saveImage(String url, Context context) {
        Context applicationContext = context.getApplicationContext();
        String fileName = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
        String extension = url.substring(url.lastIndexOf(".") + 1);
        try {
            OkHttpClient okHttpClient = new OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url).get().build();
            Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            InputStream respContent = response.body().byteStream();

            ContentValues newQrCodeDetails = new ContentValues();
            newQrCodeDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "tieba");
            newQrCodeDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            newQrCodeDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + extension);
            ContentResolver resolver = applicationContext.getContentResolver();
            Uri qrCodeUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newQrCodeDetails);
            ParcelFileDescriptor descriptor = resolver.openFileDescriptor(qrCodeUri, "w");
            IO.copyFile(respContent, new FileOutputStream(descriptor.getFileDescriptor()));
            Looper.prepare();
            Toast.makeText(applicationContext, fileName + "." + extension, Toast.LENGTH_SHORT).show();
            Looper.loop();
        } catch (IOException e) {
            return -1;
        }
        return 0;
    }
}