package gm.tieba.tabswitch.hooker.deobfuscation;

import android.content.Context;

import androidx.annotation.NonNull;

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.ClassDefItem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResStringValue;
import brut.androlib.res.decoder.ARSCDecoder;
import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.dao.AcRule;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.util.FileUtils;
import io.reactivex.rxjava3.subjects.PublishSubject;
import kotlin.collections.CollectionsKt;

public class Deobfuscation {
    private File packageResource;
    private File[] dexs;
    private int dexCount;
    private final List<Matcher> matchers = new ArrayList<>();
    private DexBakSearcher searcher;
    private final SearchScope scope = new SearchScope();

    public void setMatchers(List<Matcher> matchers) {
        this.matchers.clear();
        this.matchers.addAll(matchers);
    }

    public void unzip(PublishSubject<Float> _progress, Context context) throws IOException {
        packageResource = new File(context.getPackageResourcePath());
        var dexDir = new File(context.getCacheDir(), "app_dex");
        FileUtils.deleteRecursively(dexDir);
        dexDir.mkdirs();

        var sizeToZipEntryMatcher = new HashMap<Long, ZipEntryMatcher>();
        for (var matcher : matchers) {
            if (matcher instanceof ZipEntryMatcher) {
                var zipEntryMatcher = (ZipEntryMatcher) matcher;
                sizeToZipEntryMatcher.put(zipEntryMatcher.getSize(), zipEntryMatcher);
            }
        }

        var zipFile = new ZipFile(packageResource);
        var enumeration = zipFile.entries();
        var entryCount = 0;
        var entrySize = zipFile.size();
        while (enumeration.hasMoreElements()) {
            entryCount++;
            _progress.onNext((float) entryCount / entrySize);

            var ze = enumeration.nextElement();
            if (ze.getName().matches("classes[0-9]*?\\.dex")) {
                FileUtils.copy(zipFile.getInputStream(ze), new File(dexDir, ze.getName()));
            } else {
                var matcher = sizeToZipEntryMatcher.get(ze.getSize());
                if (matcher != null) {
                    matcher.setEntryName(ze.getName());
                }
            }
        }
        zipFile.close();

        var fs = dexDir.listFiles();
        if (fs == null) {
            throw new FileNotFoundException("解压失败");
        }
        Arrays.sort(fs, Comparator.comparingInt(it -> {
            try {
                return Integer.parseInt(it.getName().replaceAll("[a-z.]", ""));
            } catch (NumberFormatException e) {
                return 1;
            }
        }));
        dexs = fs;
        dexCount = dexs.length;
    }

    public void decodeArsc() throws IOException, AndrolibException {
        var strToResMatcher = new HashMap<String, ResMatcher>();
        var entryNameToZipEntryMatcher = new HashMap<String, ZipEntryMatcher>();
        for (var matcher : matchers) {
            if (matcher instanceof ResMatcher) {
                if (matcher instanceof ZipEntryMatcher) {
                    var zipEntryMatcher = (ZipEntryMatcher) matcher;
                    entryNameToZipEntryMatcher.put(zipEntryMatcher.getEntryName(), zipEntryMatcher);
                } else {
                    strToResMatcher.put(matcher.toString(), (ResMatcher) matcher);
                }
            }
        }

        var zipFile = new ZipFile(packageResource);
        var ze = zipFile.getEntry("resources.arsc");
        try (var in = zipFile.getInputStream(ze)) {
            var pkg = ARSCDecoder.decode(in, true, true).getOnePackage();
            pkg.listResSpecs().forEach(resResSpec -> {
                if (resResSpec.hasDefaultResource()) {
                    try {
                        var resValue = resResSpec.getDefaultResource().getValue();
                        if (resValue instanceof ResStringValue) {
                            var str = ((ResStringValue) resValue).encodeAsResXmlValue();
                            var matcher = strToResMatcher.get(str);
                            if (matcher != null) {
                                matcher.setId(resResSpec.getId().id);
                            }
                        } else if (resValue instanceof ResFileValue) {
                            var path = resValue.toString();
                            var matcher = entryNameToZipEntryMatcher.get(path);
                            if (matcher != null) {
                                matcher.setId(resResSpec.getId().id);
                            }
                        }
                    } catch (AndrolibException e) {
                        // should not happen
                    }
                }
            });
        }
        zipFile.close();
    }

    public SearchScope fastSearchAndFindScope(PublishSubject<Float> _progress) throws IOException {
        searcher = new DexBakSearcher(matchers);
        var progress = 0F;
        for (var f : dexs) {
            try (var in = new BufferedInputStream(new FileInputStream(f))) {
                var dex = DexBackedDexFile.fromInputStream(null, in);
                var classDefs = new ArrayList<>(dex.getClasses());
                for (int i = 0, classCount = classDefs.size(); i < classCount; i++) {
                    progress += (float) 1 / dexCount / classCount;
                    _progress.onNext(progress);

                    searcher.searchStringAndLiteral(classDefs.get(i), new DexBakSearcher.MatcherListener() {
                        @Override
                        public void onMatch(@NonNull Matcher matcher, @NonNull String clazz, @NonNull String method) {
                            AcRules.putRule(matcher.toString(), clazz, method);
                        }
                    });
                }
            }
        }

        // find repackageclasses
        var segments = new ArrayList<ArrayList<String>>();
        AcRules.sDao.getAll().stream().map(AcRule::getClazz).forEach(cls -> {
            var splits = cls.split("\\.");
            for (int i = 0, length = splits.length; i < length; i++) {
                var split = splits[i];
                if (segments.size() <= i) {
                    segments.add(new ArrayList<>());
                }
                segments.get(i).add(split);
            }
        });
        StringBuilder repackageclasses = new StringBuilder("L");
        for (var segment : segments) {
            var most = gm.tieba.tabswitch.util.CollectionsKt.most(segment);
            if (CollectionsKt.count(segment, s -> s.equals(most)) < segments.get(0).size() / 2) {
                break;
            }
            repackageclasses.append(most).append("/");
        }
        scope.pkg = repackageclasses.toString();

        var numberOfClassesNeedToSearch = new int[dexCount];
        for (var i = 0; i < dexCount; i++) {
            try (var in = new BufferedInputStream(new FileInputStream(dexs[i]))) {
                var dex = DexBackedDexFile.fromInputStream(null, in);
                var classDefs = ClassDefItem.getClasses(dex);
                var count = 0;
                for (var classDef : classDefs) {
                    if (scope.isInScope(classDef)) {
                        count++;
                    }
                }
                numberOfClassesNeedToSearch[i] = count;
            }
        }
        scope.numberOfClassesNeedToSearch = numberOfClassesNeedToSearch;
        return new SearchScope(scope);
    }

    public void searchSmali(PublishSubject<Float> _progress) throws IOException {
        var searchedClassCount = 0;
        var totalClassesNeedToSearch = Arrays.stream(scope.numberOfClassesNeedToSearch).sum();
        for (var i = 0; i < dexCount; i++) {
            if (scope.numberOfClassesNeedToSearch[i] == 0) {
                continue;
            }
            try (var in = new BufferedInputStream(new FileInputStream(dexs[i]))) {
                var dex = DexBackedDexFile.fromInputStream(null, in);
                var classDefs = new ArrayList<>(dex.getClasses());
                for (var classDef : classDefs) {
                    var signature = classDef.getType();
                    if (scope.isInScope(signature)) {
                        searchedClassCount++;
                        _progress.onNext((float) searchedClassCount / totalClassesNeedToSearch);

                        searcher.searchSmali(classDef, new DexBakSearcher.MatcherListener() {
                            @Override
                            public void onMatch(@NonNull Matcher matcher, @NonNull String clazz, @NonNull String method) {
                                AcRules.putRule(matcher.toString(), clazz, method);
                            }
                        });
                    }
                }
            }
        }
    }

    public void saveDexSignatureHashCode() throws IOException {
        try (var in = new FileInputStream(dexs[0])) {
            var signatureHashCode = Arrays.hashCode(DeobfuscationHelper.calcSignature(in));
            Preferences.putSignature(signatureHashCode);
        }
    }

    public static class SearchScope {
        public String pkg;
        int[] numberOfClassesNeedToSearch;

        SearchScope() {
        }

        SearchScope(SearchScope scope) {
            pkg = scope.pkg;
            numberOfClassesNeedToSearch = scope.numberOfClassesNeedToSearch;
        }

        boolean isInScope(String classDef) {
            return classDef.startsWith(pkg);
        }
    }
}
