package gm.tieba.tabswitch.hooker.anticonfusion;

import android.database.sqlite.SQLiteDatabase;

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.ClassDefItem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.ZipFile;

import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.CollectionsKt;
import gm.tieba.tabswitch.util.FileUtils;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class AntiConfusionViewModel {
    private final PublishSubject<Float> _progress = PublishSubject.create();
    Observable<Float> progress = _progress;
    private File[] dexs;

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
        Arrays.sort(fs, (o1, o2) -> {
            var ints = new int[2];
            var comparingFiles = new File[]{o1, o2};
            for (var i = 0; i < comparingFiles.length; i++) {
                try {
                    ints[i] = Integer.parseInt(comparingFiles[i].getName().replaceAll("[a-z.]", ""));
                } catch (NumberFormatException e) {
                    ints[i] = 1;
                }
            }
            return ints[0] - ints[1];
        });
        dexs = fs;
    }

    public SearchScope searchStringAndFindScope(DexBakSearcher searcher, SQLiteDatabase db) throws Throwable {
        // special optimization for TbDialog
        var dialogMatcher = "\"Dialog must be created by function create()!\"";
        var dialogClasses = new HashSet<String>();
        var progress = 0f;
        for (var f : dexs) {
            try (var in = new BufferedInputStream(new FileInputStream(f))) {
                var dex = DexBackedDexFile.fromInputStream(null, in);
                var classDefs = new ArrayList<>(dex.getClasses());
                for (var i = 0; i < classDefs.size(); i++) {
                    progress += (float) 1 / dexs.length / classDefs.size();
                    _progress.onNext(progress);

                    searcher.searchString(classDefs.get(i), (matcher, clazz, method1) -> {
                        if (matcher.equals(dialogMatcher)) {
                            dialogClasses.add(searcher.revert(clazz));
                        } else {
                            AcRules.putRule(db, matcher, clazz, method1);
                        }
                    });
                }
            }
        }

        var stringClasses = new ArrayList<String>();
        try (var c = db.query("rules", null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                stringClasses.add(c.getString(2));
            }
        }
        var first = new ArrayList<String>();
        var second = new ArrayList<String>();
        var third = new ArrayList<String>();
        stringClasses.forEach(s -> {
            var split = s.split("\\.");
            first.add(split[0]);
            second.add(split[1]);
            third.add(split[2]);
        });
        var most = "L" + CollectionsKt.most(first) + "/" + CollectionsKt.most(second) + "/" + CollectionsKt.most(third) + "/";
        var scope = new SearchScope(most, dialogClasses, new int[0]);

        var numberOfClassesNeedToSearch = new int[dexs.length];
        for (var i = 0; i < dexs.length; i++) {
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

    public void searchSmali(DexBakSearcher searcher, SearchScope scope, SQLiteDatabase db) throws Throwable {
        var searchedClassCount = 0;
        var totalClassesNeedToSearch = Arrays.stream(scope.getNumberOfClassesNeedToSearch()).sum();
        for (var i = 0; i < dexs.length; i++) {
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

                        searcher.searchSmali(classDef, (matcher, clazz, method2) ->
                                AcRules.putRule(db, matcher, clazz, method2));
                    }
                }
            }
        }
    }

    public int getDexSignatureHashCode() throws IOException {
        try (var in = new FileInputStream(dexs[0])) {
            return Arrays.hashCode(AntiConfusionHelper.calcSignature(in));
        }
    }
}
