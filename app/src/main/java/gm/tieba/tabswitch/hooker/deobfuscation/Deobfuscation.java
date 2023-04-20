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

    public void setMatchers(final List<Matcher> matchers) {
        this.matchers.clear();
        this.matchers.addAll(matchers);
    }

    public void unzip(final PublishSubject<Float> _progress, final Context context) throws IOException {
        packageResource = new File(context.getPackageResourcePath());
        final var dexDir = new File(context.getCacheDir(), "app_dex");
        FileUtils.deleteRecursively(dexDir);
        dexDir.mkdirs();

        final var sizeToZipEntryMatcher = new HashMap<Long, ZipEntryMatcher>();
        for (final var matcher : matchers) {
            if (matcher instanceof final ZipEntryMatcher zipEntryMatcher) {
                sizeToZipEntryMatcher.put(zipEntryMatcher.getSize(), zipEntryMatcher);
            }
        }

        final var zipFile = new ZipFile(packageResource);
        final var enumeration = zipFile.entries();
        var entryCount = 0;
        final var entrySize = zipFile.size();
        while (enumeration.hasMoreElements()) {
            entryCount++;
            _progress.onNext((float) entryCount / entrySize);

            final var ze = enumeration.nextElement();
            if (ze.getName().matches("classes[0-9]*?\\.dex")) {
                FileUtils.copy(zipFile.getInputStream(ze), new File(dexDir, ze.getName()));
            } else {
                final var matcher = sizeToZipEntryMatcher.get(ze.getSize());
                if (matcher != null) {
                    matcher.setEntryName(ze.getName());
                }
            }
        }
        zipFile.close();

        final var fs = dexDir.listFiles();
        if (fs == null) {
            throw new FileNotFoundException("解压失败");
        }
        Arrays.sort(fs, Comparator.comparingInt(it -> {
            try {
                return Integer.parseInt(it.getName().replaceAll("[a-z.]", ""));
            } catch (final NumberFormatException e) {
                return 1;
            }
        }));
        dexs = fs;
        dexCount = dexs.length;
    }

    public void decodeArsc() throws IOException, AndrolibException {
        final var strToResMatcher = new HashMap<String, ResMatcher>();
        final var entryNameToZipEntryMatcher = new HashMap<String, ZipEntryMatcher>();
        for (final var matcher : matchers) {
            if (matcher instanceof ResMatcher) {
                if (matcher instanceof final ZipEntryMatcher zipEntryMatcher) {
                    entryNameToZipEntryMatcher.put(zipEntryMatcher.getEntryName(), zipEntryMatcher);
                } else {
                    strToResMatcher.put(matcher.toString(), (ResMatcher) matcher);
                }
            }
        }

        final var zipFile = new ZipFile(packageResource);
        final var ze = zipFile.getEntry("resources.arsc");
        try (final var in = zipFile.getInputStream(ze)) {
            final var pkg = ARSCDecoder.decode(in, true, true).getOnePackage();
            pkg.listResSpecs().forEach(resResSpec -> {
                if (resResSpec.hasDefaultResource()) {
                    try {
                        final var resValue = resResSpec.getDefaultResource().getValue();
                        if (resValue instanceof final ResStringValue resStringValue) {
                            final var str = resStringValue.encodeAsResXmlValue();
                            final var matcher = strToResMatcher.get(str);
                            if (matcher != null) {
                                matcher.setId(resResSpec.getId().id);
                            }
                        } else if (resValue instanceof ResFileValue) {
                            final var path = resValue.toString();
                            final var matcher = entryNameToZipEntryMatcher.get(path);
                            if (matcher != null) {
                                matcher.setId(resResSpec.getId().id);
                            }
                        }
                    } catch (final AndrolibException e) {
                        // should not happen
                    }
                }
            });
        }
        zipFile.close();
    }

    public SearchScope fastSearchAndFindScope(final PublishSubject<Float> _progress) throws IOException {
        searcher = new DexBakSearcher(matchers);
        var progress = 0F;
        for (final var f : dexs) {
            try (final var in = new BufferedInputStream(new FileInputStream(f))) {
                final var dex = DexBackedDexFile.fromInputStream(null, in);
                final var classDefs = new ArrayList<>(dex.getClasses());
                for (int i = 0, classCount = classDefs.size(); i < classCount; i++) {
                    progress += (float) 1 / dexCount / classCount;
                    _progress.onNext(progress);

                    searcher.searchStringAndLiteral(classDefs.get(i), new DexBakSearcher.MatcherListener() {
                        @Override
                        public void onMatch(@NonNull final Matcher matcher, @NonNull final String clazz, @NonNull final String method) {
                            AcRules.putRule(matcher.toString(), clazz, method);
                        }
                    });
                }
            }
        }

        // find repackageclasses
        final var segments = new ArrayList<ArrayList<String>>();
        AcRules.sDao.getAll().stream().map(AcRule::getClazz).forEach(cls -> {
            final var splits = cls.split("\\.");
            for (int i = 0, length = splits.length; i < length; i++) {
                final var split = splits[i];
                if (segments.size() <= i) {
                    segments.add(new ArrayList<>());
                }
                segments.get(i).add(split);
            }
        });
        final var repackageclasses = new StringBuilder("L");
        for (final var segment : segments) {
            final var most = gm.tieba.tabswitch.util.CollectionsKt.most(segment);
            if (CollectionsKt.count(segment, s -> s.equals(most)) < segments.get(0).size() / 2) {
                break;
            }
            repackageclasses.append(most).append("/");
        }
        scope.pkg = repackageclasses.toString();

        final var numberOfClassesNeedToSearch = new int[dexCount];
        for (var i = 0; i < dexCount; i++) {
            try (final var in = new BufferedInputStream(new FileInputStream(dexs[i]))) {
                final var dex = DexBackedDexFile.fromInputStream(null, in);
                final var classDefs = ClassDefItem.getClasses(dex);
                var count = 0;
                for (final var classDef : classDefs) {
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

    public void searchSmali(final PublishSubject<Float> _progress) throws IOException {
        var searchedClassCount = 0;
        final var totalClassesNeedToSearch = Arrays.stream(scope.numberOfClassesNeedToSearch).sum();
        for (var i = 0; i < dexCount; i++) {
            if (scope.numberOfClassesNeedToSearch[i] == 0) {
                continue;
            }
            try (final var in = new BufferedInputStream(new FileInputStream(dexs[i]))) {
                final var dex = DexBackedDexFile.fromInputStream(null, in);
                final var classDefs = new ArrayList<>(dex.getClasses());
                for (final var classDef : classDefs) {
                    final var signature = classDef.getType();
                    if (scope.isInScope(signature)) {
                        searchedClassCount++;
                        _progress.onNext((float) searchedClassCount / totalClassesNeedToSearch);

                        searcher.searchSmali(classDef, new DexBakSearcher.MatcherListener() {
                            @Override
                            public void onMatch(@NonNull final Matcher matcher, @NonNull final String clazz, @NonNull final String method) {
                                AcRules.putRule(matcher.toString(), clazz, method);
                            }
                        });
                    }
                }
            }
        }
    }

    public void saveDexSignatureHashCode() throws IOException {
        try (final var in = new FileInputStream(dexs[0])) {
            final var signatureHashCode = Arrays.hashCode(DeobfuscationHelper.calcSignature(in));
            Preferences.putSignature(signatureHashCode);
        }
    }

    public static class SearchScope {
        public String pkg;
        int[] numberOfClassesNeedToSearch;

        SearchScope() {
        }

        SearchScope(final SearchScope scope) {
            pkg = scope.pkg;
            numberOfClassesNeedToSearch = scope.numberOfClassesNeedToSearch;
        }

        boolean isInScope(final String classDef) {
            return classDef.startsWith(pkg);
        }
    }
}
