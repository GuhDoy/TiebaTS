package gm.tieba.tabswitch.hooker.deobfuscation;

import android.content.Context;
import android.text.TextUtils;

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
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.value.ResStringValue;
import brut.androlib.res.decoder.ARSCDecoder;
import de.robv.android.xposed.XposedBridge;
import gm.tieba.tabswitch.dao.AcRule;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.util.FileUtils;
import gm.tieba.tabswitch.util.StringsKt;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import kotlin.collections.CollectionsKt;

public class DeobfuscationViewModel {
    private final PublishSubject<Float> _progress = PublishSubject.create();
    Observable<Float> progress = _progress;
    private File[] dexs;
    private int dexCount;

    public void unzip(File source, File destination) throws IOException {
        FileUtils.deleteRecursively(destination);
        destination.mkdirs();
        var zipFile = new ZipFile(source);
        var enumeration = zipFile.entries();
        var entryCount = 0;
        var entrySize = zipFile.size();
        while (enumeration.hasMoreElements()) {
            entryCount++;
            _progress.onNext((float) entryCount / entrySize);

            var ze = enumeration.nextElement();
            if (ze.getName().matches("classes[0-9]*?\\.dex")) {
                FileUtils.copy(zipFile.getInputStream(ze), new File(destination, ze.getName()));
            }
        }

        var fs = destination.listFiles();
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

    public static Map<Integer, String> resolveIdentifier(List<String> source, Context context) {
        var result = new HashMap<Integer, String>(source.size());
        var resources = context.getResources();
        var defPackage = context.getPackageName();
        source.forEach(it -> {
            var defType = StringsKt.substringBetween(it, "R$", ";->", "");
            if (!TextUtils.isEmpty(defType)) {
                var name = StringsKt.substringBetween(it, ";->", ":I", "");
                var identifier = resources.getIdentifier(name, defType, defPackage);
                if (identifier != 0) {
                    result.put(identifier, it);
                }
            }
        });
        return result;
    }

    public static Map<Integer, String> decodeArsc(Set<String> source, File packageResource)
            throws IOException, AndrolibException {
        var result = new HashMap<Integer, String>(source.size());
        var zipFile = new ZipFile(packageResource);
        var ze = zipFile.getEntry("resources.arsc");
        try (var in = zipFile.getInputStream(ze)) {
            var pkg = ARSCDecoder.decode(in, true, true).getOnePackage();
            pkg.listResSpecs().stream()
                    .filter(resResSpec -> "string".equals(resResSpec.getType().toString()))
                    .forEach(resResSpec -> {
                        try {
                            var maybeStr = resResSpec.getDefaultResource().getValue();
                            if (maybeStr instanceof ResStringValue) {
                                var str = ((ResStringValue) maybeStr).encodeAsResXmlValue();
                                if (source.contains(str)) {
                                    result.put(resResSpec.getId().id, str);
                                }
                            }
                        } catch (AndrolibException e) {
                            XposedBridge.log(e);
                        }
                    });
        }
        return result;
    }

    public SearchScope fastSearchAndFindScope(DexBakSearcher searcher, Map<Integer, String> idToMatcher) throws IOException {
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
                        public void onMatch(@NonNull String matcher, @NonNull String clazz, @NonNull String method) {
                            AcRules.putRule(matcher, clazz, method);
                        }

                        @Override
                        public void onMatch(long matcher, @NonNull String clazz, @NonNull String method) {
                            AcRules.putRule(idToMatcher.get((int) matcher), clazz, method);
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
        var scope = new SearchScope(repackageclasses.toString());

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
        scope.setNumberOfClassesNeedToSearch(numberOfClassesNeedToSearch);
        return scope;
    }

    public void searchSmali(DexBakSearcher searcher, SearchScope scope) throws IOException {
        var searchedClassCount = 0;
        var totalClassesNeedToSearch = Arrays.stream(scope.getNumberOfClassesNeedToSearch()).sum();
        for (var i = 0; i < dexCount; i++) {
            if (scope.getNumberOfClassesNeedToSearch()[i] == 0) {
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
                            public void onMatch(@NonNull String matcher, @NonNull String clazz, @NonNull String method) {
                                AcRules.putRule(matcher, clazz, method);
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
}
