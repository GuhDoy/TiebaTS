package gm.tieba.tabswitch.hooker.add;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.util.FileUtils;
import gm.tieba.tabswitch.util.ReflectUtils;
import gm.tieba.tabswitch.widget.TbToast;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class SaveImages extends XposedContext implements IHooker {
    private List<String> mList;
    private String mTitle;

    public void hook() throws Throwable {
        for (Method method : XposedHelpers.findClass("com.baidu.tbadk.coreExtra.view.ImagePagerAdapter",
                sClassLoader).getDeclaredMethods()) {
            if (Arrays.toString(method.getParameterTypes()).equals("[class java.util.ArrayList]")) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mList = (ArrayList<String>) param.args[0];
                    }
                });
            }
        }
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.widget.richText.TbRichText",
                sClassLoader, "toString", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        if (param.getResult() != null) mTitle = (String) param.getResult();
                    }
                });
        XposedHelpers.findAndHookConstructor("com.baidu.tbadk.coreExtra.view.ImageViewerBottomLayout",
                sClassLoader, Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        ReflectUtils.handleObjectFields(param.thisObject, ImageView.class, objField -> {
                            ImageView iv = (ImageView) objField;
                            if (iv.getId() == ReflectUtils.getId("download_icon")) {
                                Context context = ((Context) param.args[0]).getApplicationContext();
                                iv.setOnLongClickListener((v -> {
                                    TbToast.showTbToast(String.format(Locale.getDefault(),
                                            "开始下载%d张图片", mList.size()), TbToast.LENGTH_SHORT);
                                    new Thread(() -> {
                                        try {
                                            for (int i = 0; i < mList.size(); i++) {
                                                String url = mList.get(i);
                                                try {
                                                    url = "http://tiebapic.baidu.com/forum/pic/item/"
                                                            + url.substring(url.lastIndexOf("/") + 1);
                                                    url = url.substring(0, url.lastIndexOf("*"));
                                                } catch (StringIndexOutOfBoundsException ignored) {
                                                }
                                                saveImage(url, i, context);
                                            }
                                            new Handler(Looper.getMainLooper()).post(() ->
                                                    TbToast.showTbToast(String.format(Locale.getDefault(),
                                                            "已保存%d张图片至手机相册", mList.size()),
                                                            TbToast.LENGTH_SHORT));
                                        } catch (IOException | NullPointerException e) {
                                            new Handler(Looper.getMainLooper()).post(() ->
                                                    TbToast.showTbToast("保存失败", TbToast.LENGTH_SHORT));
                                        }
                                    }).start();
                                    return true;
                                }));
                                return true;
                            }
                            return false;
                        });
                    }
                });
    }

    private void saveImage(String url, int i, Context context) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder().url(url).get().build();
        Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        InputStream in = response.body().byteStream();
        ByteArrayOutputStream baos = FileUtils.cloneInputStream(in);
        String extension = FileUtils.getExtension(baos);
        in = new ByteArrayInputStream(baos.toByteArray());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues newImageDetails = new ContentValues();
            newImageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + "tieba" + File.separator + mTitle);
            newImageDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, String.valueOf(i));
            newImageDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + extension);
            ContentResolver resolver = context.getContentResolver();
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newImageDetails);
            ParcelFileDescriptor descriptor = resolver.openFileDescriptor(imageUri, "w");
            FileUtils.copy(in, descriptor.getFileDescriptor());
        } else {
            File imageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES) + File.separator + "tieba" + File.separator + mTitle);
            imageDir.mkdirs();
            FileUtils.copy(in, new File(imageDir.getPath(), i + "." + extension));

            Intent scanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
            scanIntent.setData(Uri.fromFile(imageDir));
            context.sendBroadcast(scanIntent);
        }
    }
}
