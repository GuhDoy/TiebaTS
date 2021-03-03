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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
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
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.coreExtra.view.ImagePagerAdapter", classLoader, "setData", ArrayList.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                arrayList = (ArrayList<String>) param.args[0];
            }
        });
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
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url).get().build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            private ByteArrayOutputStream baos;

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                XposedBridge.log(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                InputStream respContent = response.body().byteStream();
                String extension = getExtension(respContent);

                respContent = new ByteArrayInputStream(baos.toByteArray());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues newImageDetails = new ContentValues();
                    newImageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "tieba" + File.separator + title);
                    newImageDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, String.valueOf(i));
                    newImageDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + extension);
                    ContentResolver resolver = context.getContentResolver();
                    Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newImageDetails);
                    ParcelFileDescriptor descriptor = resolver.openFileDescriptor(imageUri, "w");
                    IO.copyFile(respContent, new FileOutputStream(descriptor.getFileDescriptor()));
                } else {
                    File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "tieba" + File.separator + title);
                    imageDir.mkdirs();
                    IO.copyFileFromStream(respContent, imageDir.getPath() + File.separator + i + "." + extension);

                    Intent scanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
                    scanIntent.setData(Uri.fromFile(imageDir));
                    context.sendBroadcast(scanIntent);
                }
                baos = null;
            }

            private String getExtension(InputStream inputStream) throws IOException {
                baos = IO.cloneInputStream(inputStream);
                inputStream = new ByteArrayInputStream(baos.toByteArray());
                byte[] buffer = new byte[10];
                if (inputStream.read(buffer) == -1) throw new IOException();
                if (Arrays.equals(buffer, new byte[]{-1, -40, -1, -32, 0, 16, 74, 70, 73, 70}))
                    return "jpg";
                else if (Arrays.equals(buffer, new byte[]{-119, 80, 78, 71, 13, 10, 26, 10, 0, 0}))
                    return "png";
                else return "gif";
            }
        });
    }
}