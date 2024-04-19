package gm.tieba.tabswitch.hooker.deobfuscation;

import android.content.Context;

import java.io.IOException;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class DeobfuscationViewModel {
    private final PublishSubject<Float> _progress = PublishSubject.create();
    public final Observable<Float> progress = _progress;
    private final Deobfuscation deobfuscation = new Deobfuscation();

    public void deobfuscate(final Context context, final List<Matcher> matchers) throws IOException {
        deobfuscation.setMatchers(matchers);
        deobfuscation.dexkit(_progress, context);
        deobfuscation.saveDexSignatureHashCode();
    }
}
