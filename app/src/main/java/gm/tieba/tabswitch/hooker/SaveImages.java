package gm.tieba.tabswitch.hooker;

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

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.IHooker;
import gm.tieba.tabswitch.hooker.model.TbToast;
import gm.tieba.tabswitch.util.IO;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class SaveImages extends BaseHooker implements IHooker {
    private ArrayList<String> mArrayList;
    private String mTitle;

    public void hook() throws Throwable {
        for (Method method : sClassLoader.loadClass("com.baidu.tbadk.coreExtra.view.ImagePagerAdapter").getDeclaredMethods()) {
            if (Arrays.toString(method.getParameterTypes()).equals("[class java.util.ArrayList]")) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mArrayList = (ArrayList<String>) param.args[0];
                    }
                });
            }
        }
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.widget.richText.TbRichText", sClassLoader, "toString", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (param.getResult() != null) mTitle = (String) param.getResult();
            }
        });
        XposedHelpers.findAndHookConstructor("com.baidu.tbadk.coreExtra.view.ImageViewerBottomLayout", sClassLoader, Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.get(param.thisObject) instanceof ImageView) {
                        ImageView downloadIcon = (ImageView) field.get(param.thisObject);
                        if (downloadIcon.getId() != sClassLoader.loadClass("com.baidu.tieba.R$id").getField("download_icon").getInt(null)) {
                            continue;
                        }
                        Context context = ((Context) param.args[0]).getApplicationContext();
                        downloadIcon.setOnLongClickListener((v -> {
                            for (int i = 0; i < mArrayList.size(); i++) {
                                String url = mArrayList.get(i);
                                try {
                                    url = "http://tiebapic.baidu.com/forum/pic/item/" + url.substring(url.lastIndexOf("/") + 1);
                                    url = url.substring(0, url.lastIndexOf("*"));
                                } catch (StringIndexOutOfBoundsException ignored) {
                                }
                                saveImage(url, i, context);
                            }
                            TbToast.showTbToast(sClassLoader, context, String.format(Locale.CHINA,
                                    "已保存%d张图片至手机相册", mArrayList.size()), TbToast.LENGTH_SHORT);
                            return true;
                        }));
                        return;
                    }
                }
            }
        });
    }

    private void saveImage(String url, int i, Context context) {
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
                InputStream in = response.body().byteStream();
                ByteArrayOutputStream baos = IO.cloneInputStream(in);
                String extension = IO.getExtension(baos);
                in = new ByteArrayInputStream(baos.toByteArray());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues newImageDetails = new ContentValues();
                    newImageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "tieba" + File.separator + mTitle);
                    newImageDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, String.valueOf(i));
                    newImageDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + extension);
                    ContentResolver resolver = context.getContentResolver();
                    Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newImageDetails);
                    ParcelFileDescriptor descriptor = resolver.openFileDescriptor(imageUri, "w");
                    IO.copy(in, descriptor.getFileDescriptor());
                } else {
                    File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "tieba" + File.separator + mTitle);
                    imageDir.mkdirs();
                    IO.copy(in, new File(imageDir.getPath(), i + "." + extension));

                    Intent scanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
                    scanIntent.setData(Uri.fromFile(imageDir));
                    context.sendBroadcast(scanIntent);
                }
            }
        });
    }
}
