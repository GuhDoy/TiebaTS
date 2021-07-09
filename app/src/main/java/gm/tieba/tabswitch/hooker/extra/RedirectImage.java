package gm.tieba.tabswitch.hooker.extra;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.XposedWrapper;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.FileUtils;

public class RedirectImage extends XposedWrapper implements IHooker {
    public void hook() throws Throwable {
        AcRules.findRule(sRes.getString(R.string.RedirectImage), (AcRules.Callback) (rule, clazz, method) -> {
            for (Method md : XposedHelpers.findClass(clazz, sClassLoader).getDeclaredMethods()) {
                switch (Arrays.toString(md.getParameterTypes())) {
                    case "[class java.lang.String, class [B, class android.content.Context]":
                        XposedBridge.hookMethod(md, new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                return saveImage((String) param.args[0], new ByteArrayInputStream(
                                        (byte[]) param.args[1]), (Context) param.args[2]);
                            }
                        });
                        break;
                    case "[class java.lang.String, class java.lang.String, class android.content.Context]":
                        XposedBridge.hookMethod(md, new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                                return saveImage((String) param.args[1], new FileInputStream(
                                        (String) param.args[0]), (Context) param.args[2]);
                            }
                        });
                        break;
                }
            }
        });
    }

    private int saveImage(String url, InputStream in, Context context) {
        Context appContext = context.getApplicationContext();
        String fileName = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
        try {
            ByteArrayOutputStream baos = FileUtils.cloneInputStream(in);
            String extension = FileUtils.getExtension(baos);
            in = new ByteArrayInputStream(baos.toByteArray());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues newImageDetails = new ContentValues();
                newImageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "tieba");
                newImageDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                newImageDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + extension);
                ContentResolver resolver = appContext.getContentResolver();
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newImageDetails);
                ParcelFileDescriptor descriptor = resolver.openFileDescriptor(imageUri, "w");
                FileUtils.copy(in, descriptor.getFileDescriptor());
            } else {
                File imageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "tieba");
                imageDir.mkdirs();
                FileUtils.copy(in, new File(imageDir.getPath(), fileName + "." + extension));

                Intent scanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
                scanIntent.setData(Uri.fromFile(imageDir));
                appContext.sendBroadcast(scanIntent);
            }
            return 0;
        } catch (IOException | NullPointerException e) {
            XposedBridge.log(e);
            return -1;
        }
    }
}
