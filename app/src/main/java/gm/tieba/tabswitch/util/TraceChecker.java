package gm.tieba.tabswitch.util;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.BuildConfig;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.hooker.TSPreferenceHelper;
import gm.tieba.tabswitch.hooker.extra.StackTrace;
import gm.tieba.tabswitch.widget.TbToast;

public class TraceChecker extends BaseHooker {
    public static int sChildCount;
    private final TSPreferenceHelper.PreferenceLayout mPreferenceLayout;
    private final String JAVA = "java";
    private final String C = "c";
    private final String S = "syscall";
    private final String FAKE = "fake";
    private int mTraceCount;

    public TraceChecker(TSPreferenceHelper.PreferenceLayout preferenceLayout) {
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
            prop();
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
        ResultBuilder result = new ResultBuilder("类加载器");
        try {
            String clazz = "de.robv.android.xposed.XposedBridge";
            PathClassLoader.getSystemClassLoader().loadClass(clazz);
            result.addTrace(JAVA, clazz);
        } catch (ClassNotFoundException ignored) {
        }

        if (Native.findXposed()) result.addTrace(C, "de/robv/android/xposed/XposedBridge");
        result.show();
    }

    private void prop() {
        ResultBuilder result = new ResultBuilder("系统属性");
        String trace = Native.prop();
        if (!trace.equals("")) result.addTrace(C, trace);
        result.show();
    }

    private void files() {
        ResultBuilder result = new ResultBuilder("文件");
        for (String symbol : new String[]{"access", "faccessat"}) {
            if (Native.inline(symbol)) result.addTrace(FAKE, symbol + " is inline hooked");
        }

        String[] paths = new String[]{getContext().getFilesDir().getParent()
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
        for (String path : paths) {
            if (Native.access(path) == 0) result.addTrace(C, path);
            if (Native.sysaccess(path) == 0) result.addTrace(S, path);
        }
        result.show();
    }

    private void maps() {
        ResultBuilder result = new ResultBuilder("内存映射");
        for (String symbol : new String[]{"open", "open64", "openat", "openat64", "__openat",
                "fopen", "fdopen"}) {
            if (Native.inline(symbol)) result.addTrace(FAKE, symbol + " is inline hooked");
        }

        String path = String.format(Locale.getDefault(), "/proc/%d/maps", Process.myPid());
        String trace = Native.fopen(path);
        if (!trace.equals("")) result.addTrace(C, trace.substring(0, trace.length() - 1));
        result.show();
    }

    private void mounts() {
        ResultBuilder result = new ResultBuilder("挂载");
        try {
            BufferedReader br = new BufferedReader(new FileReader(String.format(Locale.getDefault(),
                    "/proc/%d/mountinfo", Process.myPid())));
            List<String> paths = new ArrayList<>();
            String lastPath = getContext().getExternalFilesDir(null).getPath();
            while (!paths.contains(Environment.getExternalStorageDirectory().getPath())) {
                lastPath = IO.getParent(lastPath);
                if (!lastPath.endsWith("/data")) paths.add(lastPath);
            }

            String line;
            do {
                line = br.readLine();
                for (String path : paths) {
                    if (line != null && line.contains(String.format(" %s ", path))) {
                        result.addTrace(FAKE, line);
                    }
                }
            } while (line != null);
        } catch (IOException e) {
            XposedBridge.log(e);
            result.addTrace(FAKE, e.getMessage());
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                for (File f : getContext().getExternalFilesDir(null).getParentFile()
                        .getParentFile().listFiles()) {
                    result.addTrace(FAKE, f.getPath());
                }
            } catch (NullPointerException ignored) {
            }
        }
        result.show();
    }

    @SuppressLint({"DiscouragedPrivateApi", "PrivateApi", "QueryPermissionsNeeded"})
    private void pm() {
        ResultBuilder result = new ResultBuilder("包管理器");
        List<String> modules = new ArrayList<>();
        PackageManager pm = getContext().getPackageManager();
        try {
            IBinder service = (IBinder) Class.forName("android.os.ServiceManager")
                    .getDeclaredMethod("getService", String.class).invoke(null, "package");
            Object iPackageManager = Class.forName("android.content.pm.IPackageManager$Stub")
                    .getDeclaredMethod("asInterface", IBinder.class).invoke(null, service);
            Class<?> mPMClass = XposedHelpers.getObjectField(pm, "mPM").getClass();
            if (!mPMClass.equals(iPackageManager.getClass())) {
                result.addTrace(FAKE, mPMClass.getName());
            }
        } catch (Throwable e) {
            XposedBridge.log(e);
            result.addTrace(FAKE, e.getMessage());
        }

        for (PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
            ApplicationInfo app = pkg.applicationInfo;
            if (app.metaData != null && app.metaData.containsKey("xposedmodule")) {
                modules.add(pm.getApplicationLabel(pkg.applicationInfo).toString());
            }
        }

        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory("de.robv.android.xposed.category.MODULE_SETTINGS");
        List<ResolveInfo> ris = pm.queryIntentActivities(intentToResolve, 0);
        for (ResolveInfo ri : ris) {
            String name = ri.loadLabel(pm).toString();
            if (!modules.contains(name)) modules.add(name);
        }

        if (modules.size() > 0) {
            result.addTrace(JAVA, modules.toString());
        }
        result.show();
    }

    private void preferences() {
        ResultBuilder result = new ResultBuilder("偏好");
        for (String sp : new String[]{"TS_preferences", "TS_config", "TS_notes"}) {
            if (getContext().getSharedPreferences(sp, Context.MODE_PRIVATE)
                    .getAll().keySet().size() != 0) result.addTrace(JAVA, sp);
        }
        result.show();
    }

    private void stackTrace() {
        ResultBuilder result = new ResultBuilder("堆栈");
        for (String st : StackTrace.sStes) {
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

        private ResultBuilder(String text) {
            mResult = new StringBuilder("检测" + text + " -> ");
        }

        private void addTrace(String tag, String msg) {
            if (msg == null) return;
            mResult.append("\n").append(INDENT).append(tag).append(": ").append(msg);
            mTraceCount++;
        }

        private void show() {
            String result = mResult.toString();
            XposedBridge.log(result);
            mPreferenceLayout.addView(TSPreferenceHelper.createTextView(result));
        }
    }
}
