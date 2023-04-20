package gm.tieba.tabswitch.hooker.add;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.FileUtils;
import gm.tieba.tabswitch.util.ReflectUtils;
import gm.tieba.tabswitch.widget.TbToast;
import kotlin.text.StringsKt;

public class SaveImages extends XposedContext implements IHooker {
    private List<String> mList;
    private String mTitle;

    @NonNull
    @Override
    public String key() {
        return "save_images";
    }

    public void hook() throws Throwable {
        final var method = ReflectUtils.findFirstMethodByExactType(
                "com.baidu.tbadk.coreExtra.view.ImagePagerAdapter", ArrayList.class
        );
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                mList = (ArrayList<String>) param.args[0];
            }
        });
        XposedHelpers.findAndHookMethod("com.baidu.tbadk.widget.richText.TbRichText",
                sClassLoader, "toString", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                        if (param.getResult() != null) mTitle = (String) param.getResult();
                    }
                });
        XposedHelpers.findAndHookConstructor("com.baidu.tbadk.coreExtra.view.ImageViewerBottomLayout",
                sClassLoader, Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                        final var context = ((Context) param.args[0]).getApplicationContext();
                        // R.id.download_icon
                        final var imageView = (ImageView) ReflectUtils.getObjectField(param.thisObject, 11);
                        imageView.setOnLongClickListener(v -> {
                            TbToast.showTbToast(String.format(Locale.CHINA,
                                    "开始下载%d张图片", mList.size()), TbToast.LENGTH_SHORT);
                            new Thread(() -> {
                                try {
                                    final var list = new ArrayList<>(mList);
                                    final var title = mTitle;
                                    for (var i = 0; i < list.size(); i++) {
                                        var url = list.get(i);
                                        url = StringsKt.substringBeforeLast(url, "*", url);
                                        saveImage(url, title, i, context);
                                    }
                                    new Handler(Looper.getMainLooper()).post(() ->
                                            TbToast.showTbToast(String.format(Locale.CHINA,
                                                            "已保存%d张图片至手机相册", list.size()),
                                                    TbToast.LENGTH_SHORT));
                                } catch (final IOException | NullPointerException e) {
                                    new Handler(Looper.getMainLooper()).post(() ->
                                            TbToast.showTbToast("保存失败", TbToast.LENGTH_SHORT));
                                }
                            }).start();
                            return true;
                        });
                    }
                });
    }

    private static void saveImage(final String url, final String title, final int i, final Context context) throws IOException {
        try (final var is = new URL(url).openStream()) {
            final var bb = FileUtils.toByteBuffer(is);
            final var imageDetails = new ContentValues();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "tieba" + File.separator + title);
            } else {
                final var path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "tieba" + File.separator + title);
                path.mkdirs();
                imageDetails.put(MediaStore.MediaColumns.DATA, path + File.separator
                        + String.format(Locale.CHINA, "%02d", i) + "." + FileUtils.getExtension(bb));
            }
            imageDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, String.format(Locale.CHINA, "%02d", i));
            imageDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + FileUtils.getExtension(bb));
            final var resolver = context.getContentResolver();
            final var imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails);
            final var descriptor = resolver.openFileDescriptor(imageUri, "w");
            FileUtils.copy(bb, descriptor.getFileDescriptor());
        }
    }
}
