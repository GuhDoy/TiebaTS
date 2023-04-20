package gm.tieba.tabswitch.hooker.extra;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.BuildConfig;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.TSPreferenceHelper;
import gm.tieba.tabswitch.util.FileUtils;
import gm.tieba.tabswitch.widget.TbToast;

public class TraceChecker extends XposedContext {
    public static int sChildCount;
    private final TSPreferenceHelper.PreferenceLayout mPreferenceLayout;
    private final String JAVA = "java";
    private final String C = "c";
    private final String S = "syscall";
    private final String FAKE = "fake";
    private int mTraceCount;

    public TraceChecker(final TSPreferenceHelper.PreferenceLayout preferenceLayout) {
        mPreferenceLayout = preferenceLayout;
    }

    public void checkAll() {
        mTraceCount = 0;
        while (mPreferenceLayout.getChildAt(sChildCount) != null) {
            mPreferenceLayout.removeViewAt(sChildCount);
        }
        getContext().getExternalFilesDir(null).mkdirs();
        if (Preferences.getBoolean("check_xposed")) {
            classloader();
        }
        if (Preferences.getBoolean("check_module")) {
            files();
            maps();
            mounts();
            pm();
            preferences();
        }
        if (Preferences.getBoolean("check_stack_trace")) stackTrace();
        TbToast.showTbToast(mTraceCount > 0 ? String.format(Locale.getDefault(), "%s\n检测出%d处痕迹",
                randomToast(), mTraceCount) : "未检测出痕迹", TbToast.LENGTH_SHORT);
    }

    private void classloader() {
        final ResultBuilder result = new ResultBuilder("类加载器");
        try {
            final String clazz = "de.robv.android.xposed.XposedBridge";
            PathClassLoader.getSystemClassLoader().loadClass(clazz);
            result.addTrace(JAVA, clazz);
        } catch (final ClassNotFoundException ignored) {
        }

        if (NativeCheck.findXposed()) result.addTrace(C, "de/robv/android/xposed/XposedBridge");
        if (NativeCheck.isFindClassInline()) result.addTrace(FAKE, "FindClass is inline hooked");
        result.show();
    }

    private void files() {
        final ResultBuilder result = new ResultBuilder("文件");
        for (final String symbol : new String[]{"access", "faccessat"}) {
            if (NativeCheck.inline(symbol)) result.addTrace(FAKE, symbol + " is inline hooked");
        }

        final String[] paths = new String[]{getContext().getFilesDir().getParent()
                .replace(getContext().getPackageName(), BuildConfig.APPLICATION_ID),
                getContext().getExternalFilesDir(null).getParent()
                        .replace(getContext().getPackageName(), BuildConfig.APPLICATION_ID),
                getContext().getDatabasePath("Rules.db").getPath(),
                getContext().getFilesDir().getParent() + File.separator + "shared_prefs"
                        + File.separator + "TS_preferences.xml",
                getContext().getFilesDir().getParent() + File.separator + "shared_prefs"
                        + File.separator + "TS_config.xml",
                getContext().getFilesDir().getParent() + File.separator + "shared_prefs"
                        + File.separator + "TS_notes.xml"};
        for (final String path : paths) {
            if (NativeCheck.access(path) == 0) result.addTrace(C, path);
        }
        result.show();
    }

    private void maps() {
        final ResultBuilder result = new ResultBuilder("内存映射");
        for (final String symbol : new String[]{"open", "open64", "openat", "openat64", "__openat",
                "fopen", "fdopen"}) {
            if (NativeCheck.inline(symbol)) result.addTrace(FAKE, symbol + " is inline hooked");
        }

        final String path = String.format(Locale.getDefault(), "/proc/%d/maps", Process.myPid());
        final String trace = NativeCheck.fopen(path);
        if (!TextUtils.isEmpty(trace)) {
            result.addTrace(C, trace.substring(0, trace.length() - 1));
        }
        result.show();
    }

    private void mounts() {
        final ResultBuilder result = new ResultBuilder("挂载");
        try {
            final BufferedReader br = new BufferedReader(new FileReader(String.format(Locale.getDefault(),
                    "/proc/%d/mountinfo", Process.myPid())));
            final List<String> paths = new ArrayList<>();
            String lastPath = getContext().getExternalFilesDir(null).getPath();
            while (!paths.contains(Environment.getExternalStorageDirectory().getPath())) {
                lastPath = FileUtils.getParent(lastPath);
                if (!lastPath.endsWith("/data")) paths.add(lastPath);
            }

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                for (final String path : paths) {
                    if (line.contains(String.format(" %s ", path))) {
                        result.addTrace(FAKE, line);
                    }
                }
            }
        } catch (final IOException e) {
            XposedBridge.log(e);
            result.addTrace(FAKE, e.getMessage());
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                for (final File f : getContext().getExternalFilesDir(null).getParentFile()
                        .getParentFile().listFiles()) {
                    result.addTrace(FAKE, f.getPath());
                }
            } catch (final NullPointerException ignored) {
            }
        }
        result.show();
    }

    @SuppressLint({"DiscouragedPrivateApi", "PrivateApi", "QueryPermissionsNeeded"})
    private void pm() {
        final ResultBuilder result = new ResultBuilder("包管理器");
        final List<String> modules = new ArrayList<>();
        final PackageManager pm = getContext().getPackageManager();
        try {
            final IBinder service = (IBinder) Class.forName("android.os.ServiceManager")
                    .getDeclaredMethod("getService", String.class).invoke(null, "package");
            final Object iPackageManager = Class.forName("android.content.pm.IPackageManager$Stub")
                    .getDeclaredMethod("asInterface", IBinder.class).invoke(null, service);
            final Field field = pm.getClass().getDeclaredField("mPM");
            field.setAccessible(true);
            final Class<?> mPMClass = field.get(pm).getClass();
            if (!mPMClass.equals(iPackageManager.getClass())) {
                result.addTrace(FAKE, mPMClass.getName());
            }
        } catch (final Throwable e) {
            XposedBridge.log(e);
            result.addTrace(FAKE, e.getMessage());
        }

        for (final PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
            final ApplicationInfo app = pkg.applicationInfo;
            if (app.metaData != null && app.metaData.containsKey("xposedmodule")) {
                modules.add(pm.getApplicationLabel(pkg.applicationInfo).toString());
            }
        }

        final Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory("de.robv.android.xposed.category.MODULE_SETTINGS");
        final List<ResolveInfo> ris = pm.queryIntentActivities(intentToResolve, 0);
        for (final ResolveInfo ri : ris) {
            final String name = ri.loadLabel(pm).toString();
            if (!modules.contains(name)) modules.add(name);
        }

        if (modules.size() > 0) {
            result.addTrace(JAVA, modules.toString());
        }
        result.show();
    }

    private void preferences() {
        final ResultBuilder result = new ResultBuilder("偏好");
        for (final String sp : new String[]{"TS_preferences", "TS_config", "TS_notes"}) {
            if (getContext().getSharedPreferences(sp, Context.MODE_PRIVATE)
                    .getAll().keySet().size() != 0) result.addTrace(JAVA, sp);
        }
        result.show();
    }

    private void stackTrace() {
        final ResultBuilder result = new ResultBuilder("堆栈");
        for (final String st : StackTrace.sStes) {
            result.addTrace(JAVA, st);
        }
        result.show();
    }

    private String randomToast() {
        switch (new Random().nextInt(9)) {
            case 0:
                return "没收尾巴球";
            case 1:
                return "尾巴捏捏";
            case 2:
                return "没收尾巴";
            case 3:
                return "点燃尾巴";
            case 4:
                return "捏尾巴";
            case 5:
                return "若要人不知，除非己莫为";
            case 6:
                return "哼！你满身都是破绽";
            case 7:
                return "checkmate";
            case 8:
                return "Xposed 无处可逃";
            default:
                return "";
        }
    }

    private class ResultBuilder {
        private static final String INDENT = "　 ";
        StringBuilder mResult;

        private ResultBuilder(final String text) {
            mResult = new StringBuilder("检测" + text + " -> ");
        }

        private void addTrace(final String tag, final String msg) {
            if (msg == null) return;
            mResult.append("\n").append(INDENT).append(tag).append(": ").append(msg);
            mTraceCount++;
        }

        private void show() {
            final String result = mResult.toString();
            XposedBridge.log(result);
            mPreferenceLayout.addView(TSPreferenceHelper.createTextView(result));
        }
    }
}
