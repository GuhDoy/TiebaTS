package gm.tieba.tabswitch.hooker.add;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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

public class SaveImages extends XposedContext implements IHooker {
    private List<String> mList;
    private String mTitle;

    public void hook() throws Throwable {
        for (var method : XposedHelpers.findClass("com.baidu.tbadk.coreExtra.view.ImagePagerAdapter",
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
                            var iv = (ImageView) objField;
                            if (iv.getId() == ReflectUtils.getId("download_icon")) {
                                var context = ((Context) param.args[0]).getApplicationContext();
                                iv.setOnLongClickListener((v -> {
                                    TbToast.showTbToast(String.format(Locale.CHINA,
                                            "开始下载%d张图片", mList.size()), TbToast.LENGTH_SHORT);
                                    new Thread(() -> {
                                        try {
                                            var list = new ArrayList<>(mList);
                                            var title = mTitle;
                                            for (var i = 0; i < list.size(); i++) {
                                                var url = list.get(i);
                                                try {
                                                    url = "http://tiebapic.baidu.com/forum/pic/item/"
                                                            + url.substring(url.lastIndexOf("/") + 1);
                                                    url = url.substring(0, url.lastIndexOf("*"));
                                                } catch (StringIndexOutOfBoundsException ignored) {
                                                }
                                                saveImage(url, title, i, context);
                                            }
                                            new Handler(Looper.getMainLooper()).post(() ->
                                                    TbToast.showTbToast(String.format(Locale.CHINA,
                                                            "已保存%d张图片至手机相册", list.size()),
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

    private static void saveImage(String url, String title, int i, Context context) throws IOException {
        try (var is = new URL(url).openStream()) {
            var bb = FileUtils.toByteBuffer(is);
            var newImageDetails = new ContentValues();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                newImageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "tieba" + File.separator + title);
            } else {
                var path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "tieba" + File.separator + title);
                path.mkdirs();
                newImageDetails.put(MediaStore.MediaColumns.DATA, path + File.separator
                        + i + "." + FileUtils.getExtension(bb));
            }
            newImageDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, String.valueOf(i));
            newImageDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + FileUtils.getExtension(bb));
            var resolver = context.getContentResolver();
            var imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newImageDetails);
            var descriptor = resolver.openFileDescriptor(imageUri, "w");
            FileUtils.copy(bb, descriptor.getFileDescriptor());
        }
    }
}
