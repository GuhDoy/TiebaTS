package gm.tieba.tabswitch.hookImpl;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;
import gm.tieba.tabswitch.util.IO;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class SaveImages extends Hook {
    private static ArrayList<String> arrayList;
    private static String title;

    public static void hook(ClassLoader classLoader) throws Throwable {
        try {
            XposedHelpers.findAndHookMethod("com.baidu.tbadk.coreExtra.view.ImagePagerAdapter", classLoader, "setData", ArrayList.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    arrayList = (ArrayList<String>) param.args[0];
                }
            });
        } catch (NoSuchMethodError e) {
            XposedHelpers.findAndHookMethod("com.baidu.tbadk.coreExtra.view.ImagePagerAdapter", classLoader, "l", ArrayList.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    arrayList = (ArrayList<String>) param.args[0];
                }
            });
        }
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.widget.richText.TbRichText", classLoader, "toString", new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (param.getResult() != null) title = (String) param.getResult();
            }
        });
        XposedHelpers.findAndHookConstructor("com.baidu.tbadk.coreExtra.view.ImageViewerBottomLayout", classLoader, Context.class, new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                Field[] fields = param.thisObject.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (field.get(param.thisObject) instanceof ImageView) {
                        ImageView downloadIcon = (ImageView) field.get(param.thisObject);
                        if (downloadIcon.getId() != classLoader.loadClass("com.baidu.tieba.R$id").getField("download_icon").getInt(null))
                            continue;
                        Context context = ((Context) param.args[0]).getApplicationContext();
                        downloadIcon.setOnLongClickListener((v -> {
                            for (int i = 0; i < arrayList.size(); i++) {
                                String url = arrayList.get(i);
                                try {
                                    url = "http://tiebapic.baidu.com/forum/pic/item/" + url.substring(url.lastIndexOf("/") + 1);
                                    url = url.substring(0, url.lastIndexOf("*"));
                                } catch (StringIndexOutOfBoundsException ignored) {
                                }
                                saveImage(url, i, context);
                            }
                            Toast.makeText(context, String.format(Locale.CHINA, "已保存%d张图片至手机相册", arrayList.size()), Toast.LENGTH_SHORT).show();
                            return true;
                        }));
                        return;
                    }
                }
            }
        });
    }

    private static void saveImage(String url, int i, Context context) {
        OkHttpClient okHttpClient = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder().url(url).get().build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                XposedBridge.log(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                InputStream inputStream = response.body().byteStream();
                ByteArrayOutputStream baos = IO.cloneInputStream(inputStream);
                String extension = IO.getExtension(baos);
                inputStream = new ByteArrayInputStream(baos.toByteArray());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues newImageDetails = new ContentValues();
                    newImageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "tieba" + File.separator + title);
                    newImageDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, String.valueOf(i));
                    newImageDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + extension);
                    ContentResolver resolver = context.getContentResolver();
                    Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newImageDetails);
                    ParcelFileDescriptor descriptor = resolver.openFileDescriptor(imageUri, "w");
                    IO.copyFile(inputStream, descriptor.getFileDescriptor());
                } else {
                    File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "tieba" + File.separator + title);
                    imageDir.mkdirs();
                    IO.copyFile(inputStream, new File(imageDir.getPath(), i + "." + extension));

                    Intent scanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
                    scanIntent.setData(Uri.fromFile(imageDir));
                    context.sendBroadcast(scanIntent);
                }
            }
        });
    }
}