package gm.tieba.tabswitch.hooker;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.ArrayMap;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.hooker.model.BaseHooker;
import gm.tieba.tabswitch.hooker.model.IHooker;
import gm.tieba.tabswitch.hooker.model.Rule;
import gm.tieba.tabswitch.util.IO;

@SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
public class StorageRedirect extends BaseHooker implements IHooker {
    private final File mTarget = sContextRef.get().getExternalCacheDir();

    public void hook() throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 通过 getService("mount"); 得到一个新的 IBinder 对象 rawBinder
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            IBinder rawBinder = (IBinder) serviceManager.getDeclaredMethod("getService",
                    String.class).invoke(null, "mount");
            // 动态代理 rawBinder 中的 queryLocalInterface 方法，使这个方法返回替换后的 IStorageManager
            IBinder binder = (IBinder) Proxy.newProxyInstance(serviceManager.getClassLoader(),
                    new Class<?>[]{IBinder.class}, new IBinderHandler(rawBinder));
            // 用准备好的 IBinder 对象替换 ServiceManager 中缓存的 IBinder 对象
            Field field = serviceManager.getDeclaredField("sCache");
            field.setAccessible(true);
            ArrayMap<String, IBinder> map = (ArrayMap<String, IBinder>) field.get(null);
            map.put("mount", binder);
        } else {
            XposedHelpers.findAndHookMethod(Environment.class, "getExternalStorageDirectory", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(mTarget);
                }
            });
        }
        Rule.findRule("0x4197d783fc000000L", new Rule.Callback() {
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
        });
    }

    private class IBinderHandler implements InvocationHandler {
        private final IBinder mBase;
        private Class<?> mIin;

        public IBinderHandler(IBinder rawBinder) {
            mBase = rawBinder;
            try {
                mIin = Class.forName("android.os.storage.IStorageManager");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("queryLocalInterface".equals(method.getName())) {
                return Proxy.newProxyInstance(mBase.getClass().getClassLoader(),
                        new Class<?>[]{mIin}, new IStorageManagerHandler(mBase));
            }
            return method.invoke(mBase, args);
        }
    }

    private class IStorageManagerHandler implements InvocationHandler {
        private Object/*IStorageManager*/ mBase;

        public IStorageManagerHandler(IBinder base) {
            try {
                mBase = Class.forName("android.os.storage.IStorageManager$Stub").getDeclaredMethod(
                        "asInterface", IBinder.class).invoke(null, base);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getVolumeList".equals(method.getName())) {
                StorageVolume[] volumes = (StorageVolume[]) method.invoke(mBase, args);
                Field field = volumes[0].getClass().getDeclaredField("mPath");
                field.setAccessible(true);
                field.set(volumes[0], mTarget);
                return volumes;
            }
            return method.invoke(mBase, args);
        }
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
                IO.copy(in, descriptor.getFileDescriptor());
            } else {
                File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "tieba");
                imageDir.mkdirs();
                IO.copy(in, new File(imageDir.getPath(), fileName + "." + extension));

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
