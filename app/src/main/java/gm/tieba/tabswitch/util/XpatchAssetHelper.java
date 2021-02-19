package gm.tieba.tabswitch.util;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class XpatchAssetHelper {
    private static char[] toChars(byte[] mSignature) {
        final int N = mSignature.length;
        final int N2 = N * 2;
        char[] text = new char[N2];
        for (int j = 0; j < N; j++) {
            byte v = mSignature[j];
            int d = (v >> 4) & 0xf;
            text[j * 2] = (char) (d >= 10 ? ('a' + d - 10) : ('0' + d));
            d = v & 0xf;
            text[j * 2 + 1] = (char) (d >= 10 ? ('a' + d - 10) : ('0' + d));
        }
        return text;
    }

    private static Certificate[] loadCertificates(JarFile jarFile, JarEntry je, byte[] readBuffer) {
        try {
            InputStream is = jarFile.getInputStream(je);
            while (is.read(readBuffer, 0, readBuffer.length) != -1)
                if (Thread.currentThread().isInterrupted()) break;
            is.close();
            return je != null ? je.getCertificates() : null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getApkSignInfo(String apkFilePath) {
        byte[] readBuffer = new byte[8192];
        Certificate[] certs = null;
        try {
            JarFile jarFile = new JarFile(apkFilePath);
            Enumeration<?> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = (JarEntry) entries.nextElement();
                if (je.isDirectory()) continue;
                if (je.getName().startsWith("META-INF/")) continue;
                Certificate[] localCerts = loadCertificates(jarFile, je, readBuffer);
                if (certs == null) certs = localCerts;
                else {
                    for (Certificate cert : certs) {
                        boolean found = false;
                        if (localCerts != null)
                            for (Certificate localCert : localCerts)
                                if (cert != null && cert.equals(localCert)) {
                                    found = true;
                                    break;
                                }
                        if (!found || certs.length != localCerts.length) {
                            jarFile.close();
                            return null;
                        }
                    }
                }
            }
            jarFile.close();
            return new String(toChars(certs[0].getEncoded()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Map<String, Object>> loadAllInstalledModule(Context context) {
        PackageManager pm = context.getPackageManager();
        List<Map<String, Object>> installedModuleList = new ArrayList<>();
        for (PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
            ApplicationInfo app = pkg.applicationInfo;
            if (!app.enabled) continue;
            if (app.metaData != null && app.metaData.containsKey("xposedmodule")) {
                String apkName = context.getPackageManager().getApplicationLabel(pkg.applicationInfo).toString();
                List<String> scopeList;
                Map<String, Object> map = new HashMap<>();
                try {
                    int scopeListResourceId = app.metaData.getInt("xposedscope");
                    if (scopeListResourceId != 0) {
                        scopeList = Arrays.asList(pm.getResourcesForApplication(app).getStringArray(scopeListResourceId));
                        map.put("isChecked", scopeList.contains(ManifestParser.packageName));
                    } else map.put("isChecked", false);
                } catch (Exception e) {
                    map.put("isChecked", false);
                }
                map.put("apkName", apkName);
                map.put("packageName", pkg.applicationInfo.packageName);
                installedModuleList.add(map);
            }
        }
        return installedModuleList;
    }
}