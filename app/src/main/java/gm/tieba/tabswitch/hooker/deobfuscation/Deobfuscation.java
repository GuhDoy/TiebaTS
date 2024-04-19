package gm.tieba.tabswitch.hooker.deobfuscation;

import android.content.Context;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassDataList;
import org.luckypray.dexkit.result.MethodDataList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

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

    public void dexkit(final PublishSubject<Float> progress, final Context context) {
        load("dexkit");
        packageResource = context.getPackageResourcePath();
        final var bridge = DexKitBridge.create(packageResource);
        Objects.requireNonNull(bridge);

        forEachProgressed(progress, matchers, matcher -> {
            MethodDataList ret = new MethodDataList();
            if (matcher.getClassMatcher() != null) {
                ClassDataList retClassList = bridge.findClass(FindClass.create().matcher(matcher.getClassMatcher().getMatcher()));
                for (var retClass: retClassList) {
                    ret.addAll(findMethod(bridge, FindMethod.create().searchPackages(retClass.getName()), matcher));
                }
            } else {
                ret.addAll(findMethod(bridge, FindMethod.create(), matcher));
            }
            for (final var methodData : ret) {
                AcRules.putRule(
                        matcher.toString(), methodData.getClassName(), methodData.getName());
            }
        });

        bridge.close();
    }

    private MethodDataList findMethod(DexKitBridge bridge, FindMethod baseMethodQuery, Matcher matcher) {
        MethodDataList ret = null;
        if (matcher instanceof final StringMatcher stringMatcher) {
            ret = bridge.findMethod(
                    baseMethodQuery.matcher(
                            MethodMatcher.create().usingStrings(stringMatcher.getStr())
                    )
            );
        } else if (matcher instanceof final ResMatcher resMatcher) {
            ret = bridge.findMethod(
                    baseMethodQuery.matcher(
                            MethodMatcher.create().usingNumbers(resMatcher.getId())
                    )
            );
        } else if (matcher instanceof final SmaliMatcher smaliMatcher) {
            ret = bridge.findMethod(
                    baseMethodQuery.matcher(
                            MethodMatcher.create().addInvoke(
                                    MethodMatcher.create().descriptor(smaliMatcher.getDescriptor())
                            )
                    )
            );
        } else if (matcher instanceof final MethodNameMatcher methodNameMatcher) {
            ret = bridge.findMethod(
                    baseMethodQuery.matcher(
                            MethodMatcher.create().name(methodNameMatcher.getName())
                    )
            );
        } else if (matcher instanceof final ReturnTypeMatcher<?> returnTypeMatcher) {
            ret = bridge.findMethod(
                    baseMethodQuery.matcher(
                            MethodMatcher.create().returnType(returnTypeMatcher.getReturnType())
                    )
            );
        }
        return ret;
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
