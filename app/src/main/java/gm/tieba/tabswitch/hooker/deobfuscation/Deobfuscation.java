package gm.tieba.tabswitch.hooker.deobfuscation;

import android.content.Context;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodDataList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResStringValue;
import brut.androlib.res.decoder.ARSCDecoder;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.dao.Preferences;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class Deobfuscation extends XposedContext {
    private String packageResource;
    private final List<Matcher> matchers = new ArrayList<>();

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

    private <T> void forEachProgressed(final PublishSubject<Float> progress,
                                       final Collection<T> collection,
                                       final Consumer<? super T> action) {
        final var size = collection.size();
        var count = 0;
        for (final T t : collection) {
            count++;
            progress.onNext((float) count / size);
            action.accept(t);
        }
    }

    public void decodeArsc(final PublishSubject<Float> progress)
            throws IOException, AndrolibException {
        progress.onNext(0F);
        final var strToResMatcher = new HashMap<String, ResMatcher>();
        final var entryNameToZipEntryMatcher = new HashMap<String, ZipEntryMatcher>();
        final var resIdentifierToResMatcher = new HashMap<String, ResIdentifierMatcher>();
        for (final var matcher : matchers) {
            if (matcher instanceof ResMatcher) {
                if (matcher instanceof final ZipEntryMatcher zipEntryMatcher) {
                    entryNameToZipEntryMatcher.put(zipEntryMatcher.getEntryName(), zipEntryMatcher);
                } else if (matcher instanceof final ResIdentifierMatcher resIdentifierMatcher) {
                    resIdentifierToResMatcher.put(resIdentifierMatcher.toString(), resIdentifierMatcher);
                } else {
                    strToResMatcher.put(matcher.toString(), (ResMatcher) matcher);
                }
            }
        }

        final var zipFile = new ZipFile(packageResource);
        final var ze = zipFile.getEntry("resources.arsc");
        try (final var in = zipFile.getInputStream(ze)) {
            final var pkg = ARSCDecoder.decode(in, true, true).getOnePackage();
            forEachProgressed(progress, pkg.listResSpecs(), resResSpec -> {
                final var identifierMatcher = resIdentifierToResMatcher.get(String.format("%s.%s", resResSpec.getType().getName(), resResSpec.getName()));
                if (identifierMatcher != null) {
                    identifierMatcher.setId(resResSpec.getId().id);
                } else if (resResSpec.hasDefaultResource()) {
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

    public void dexkit(final PublishSubject<Float> progress) {
        load("dexkit");
        final var bridge = DexKitBridge.create(packageResource);
        Objects.requireNonNull(bridge);

        forEachProgressed(progress, matchers, matcher -> {
            MethodDataList ret = null;
            if (matcher instanceof final StringMatcher stringMatcher) {
                ret = bridge.findMethod(
                        FindMethod.create().matcher(
                                MethodMatcher.create().usingStrings(stringMatcher.getStr())
                        )
                );
            } else if (matcher instanceof final ResMatcher resMatcher) {
                ret = bridge.findMethod(
                        FindMethod.create().matcher(
                                MethodMatcher.create().usingNumbers(resMatcher.getId())
                        )
                );
            } else if (matcher instanceof final SmaliMatcher smaliMatcher) {
                ret = bridge.findMethod(
                        FindMethod.create().matcher(
                                MethodMatcher.create().addInvoke(
                                        MethodMatcher.create().descriptor(smaliMatcher.toString())
                                )
                        )
                );
            }
            if (ret != null) {
                for (final var methodData : ret) {
                    AcRules.putRule(
                            matcher.toString(), methodData.getClassName(), methodData.getName());
                }
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
}
