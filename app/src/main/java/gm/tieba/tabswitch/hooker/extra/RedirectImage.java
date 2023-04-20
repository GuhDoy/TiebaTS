package gm.tieba.tabswitch.hooker.extra;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.FileUtils;

public class RedirectImage extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "redirect_image";
    }

    @Override
    public void hook() throws Throwable {
        // 0x4197d783fc000000L
        for (final var md : XposedHelpers.findClass("com.baidu.tbadk.core.util.FileHelper", sClassLoader).getDeclaredMethods()) {
            switch (Arrays.toString(md.getParameterTypes())) {
                case "[class java.lang.String, class [B, class android.content.Context]":
                    XposedBridge.hookMethod(md, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(final MethodHookParam param) throws Throwable {
                            return saveImage((String) param.args[0], new ByteArrayInputStream(
                                    (byte[]) param.args[1]), (Context) param.args[2]);
                        }
                    });
                    break;
                case "[class java.lang.String, class java.lang.String, class android.content.Context]":
                    XposedBridge.hookMethod(md, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(final MethodHookParam param) throws Throwable {
                            return saveImage((String) param.args[1], new FileInputStream(
                                    (String) param.args[0]), (Context) param.args[2]);
                        }
                    });
                    break;
            }
        }
    }

    private int saveImage(final String url, final InputStream is, final Context context) {
        final var appContext = context.getApplicationContext();
        final var fileName = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
        try {
            final var bb = FileUtils.toByteBuffer(is);
            final var imageDetails = new ContentValues();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "tieba");
            } else {
                final var path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "tieba");
                path.mkdirs();
                imageDetails.put(MediaStore.MediaColumns.DATA, path + File.separator
                        + fileName + "." + FileUtils.getExtension(bb));
            }
            imageDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            imageDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + FileUtils.getExtension(bb));
            final var resolver = appContext.getContentResolver();
            final var imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails);
            final var descriptor = resolver.openFileDescriptor(imageUri, "w");
            FileUtils.copy(bb, descriptor.getFileDescriptor());
            is.close();
            return 0;
        } catch (final IOException | NullPointerException e) {
            XposedBridge.log(e);
            return -1;
        }
    }
}
