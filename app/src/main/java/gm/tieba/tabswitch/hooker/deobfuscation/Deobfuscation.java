package gm.tieba.tabswitch.hooker.deobfuscation;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResStringValue;
import brut.androlib.res.decoder.ARSCDecoder;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRule;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.MethodInvokingArgs;
import io.luckypray.dexkit.builder.MethodUsingNumberArgs;
import io.luckypray.dexkit.builder.MethodUsingStringArgs;
import io.reactivex.rxjava3.subjects.PublishSubject;
import kotlin.collections.CollectionsKt;

public class Deobfuscation extends XposedContext {
    private String packageResource;
    private final List<Matcher> matchers = new ArrayList<>();
    private DexKitBridge bridge;
    private final SearchScope scope = new SearchScope();

    public void setMatchers(final List<Matcher> matchers) {
        this.matchers.clear();
        this.matchers.addAll(matchers);
    }

    public void unzip(final PublishSubject<Float> progress, final Context context)
            throws IOException {
        packageResource = context.getPackageResourcePath();

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
            progress.onNext((float) entryCount / entrySize);

            final var ze = enumeration.nextElement();
            final var matcher = sizeToZipEntryMatcher.get(ze.getSize());
            if (matcher != null) {
                matcher.setEntryName(ze.getName());
            }
        }
        zipFile.close();
    }

    public void decodeArsc(final PublishSubject<Float> progress)
            throws IOException, AndrolibException {
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

    public SearchScope fastSearchAndFindScope(final PublishSubject<Float> progress) {
        load("dexkit");
        bridge = DexKitBridge.create(packageResource);

        matchers.stream()
                .filter(StringMatcher.class::isInstance)
                .map(StringMatcher.class::cast)
                .forEach(matcher -> {
                    final var ret = bridge.findMethodUsingString(
                            new MethodUsingStringArgs.Builder()
                                    .usingString(matcher.getStr())
                                    .build()
                    );
                    for (final var d : ret) {
                        AcRules.putRule(matcher.toString(), d.getDeclaringClassName(), d.getName());
                    }
                });
        matchers.stream()
                .filter(ResMatcher.class::isInstance)
                .map(ResMatcher.class::cast)
                .forEach(matcher -> {
                    final var ret = bridge.findMethodUsingNumber(
                            new MethodUsingNumberArgs.Builder()
                                    .usingNumber(matcher.getId())
                                    .build()
                    );
                    for (final var d : ret) {
                        AcRules.putRule(matcher.toString(), d.getDeclaringClassName(), d.getName());
                    }
                });

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

        return new SearchScope(scope);
    }

    public void searchSmali(final PublishSubject<Float> progress) {
        matchers.stream()
                .filter(SmaliMatcher.class::isInstance)
                .map(SmaliMatcher.class::cast)
                .forEach(matcher -> {
                    final var ret = bridge.findMethodInvoking(
                            new MethodInvokingArgs.Builder()
                                    .methodDeclareClass(scope.pkg)
                                    .methodDescriptor(matcher.getStr())
                                    .build()
                    );
                    for (final var d : ret.keySet()) {
                        AcRules.putRule(matcher.toString(), d.getDeclaringClassName(), d.getName());
                    }
                });

        bridge.close();
    }

    public void saveDexSignatureHashCode() throws IOException {
        try (final var apk = new ZipFile(packageResource)) {
            try (final var in = apk.getInputStream(apk.getEntry("classes.dex"))) {
                final var signatureHashCode = Arrays.hashCode(DeobfuscationHelper.calcSignature(in));
                Preferences.putSignature(signatureHashCode);
            }
        }
    }

    public static class SearchScope {
        public String pkg;

        SearchScope() {
        }

        SearchScope(final SearchScope scope) {
            pkg = scope.pkg;
        }

        boolean isInScope(final String classDef) {
            return classDef.startsWith(pkg);
        }
    }
}
