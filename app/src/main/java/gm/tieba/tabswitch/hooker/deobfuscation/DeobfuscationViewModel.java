package gm.tieba.tabswitch.hooker.deobfuscation;

import android.content.Context;

import java.io.IOException;
import java.util.List;

import brut.androlib.AndrolibException;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class DeobfuscationViewModel {
    private final PublishSubject<Float> _progress = PublishSubject.create();
    public final Observable<Float> progress = _progress;
    private final Deobfuscation deobfuscation = new Deobfuscation();

    public void deobfuscateStep1(final Context context, final List<Matcher> matchers) throws IOException {
        deobfuscation.setMatchers(matchers);
        deobfuscation.unzip(_progress, context);
    }

    public void deobfuscateStep2() throws IOException, AndrolibException {
        deobfuscation.decodeArsc(_progress);
    }

    public void deobfuscateStep3() throws IOException {
        deobfuscation.dexkit(_progress);
        deobfuscation.saveDexSignatureHashCode();
    }
}
