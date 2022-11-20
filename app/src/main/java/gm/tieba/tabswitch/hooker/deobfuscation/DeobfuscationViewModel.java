package gm.tieba.tabswitch.hooker.deobfuscation;

import android.content.Context;

import java.io.IOException;
import java.util.List;

import brut.androlib.AndrolibException;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class DeobfuscationViewModel {
    private final PublishSubject<Float> _progress = PublishSubject.create();
    Observable<Float> progress = _progress;
    private final Deobfuscation deobfuscation = new Deobfuscation();

    public void deobfuscateStep1(Context context, List<Matcher> matchers) throws IOException {
        deobfuscation.unzip(_progress, context);
        deobfuscation.setMatchers(matchers);
    }

    public void deobfuscateStep2() throws IOException, AndrolibException {
        deobfuscation.decodeArsc();
    }

    public Deobfuscation.SearchScope deobfuscateStep3() throws IOException {
        return deobfuscation.fastSearchAndFindScope(_progress);
    }

    public void deobfuscateStep4() throws IOException {
        deobfuscation.searchSmali(_progress);
        deobfuscation.saveDexSignatureHashCode();
    }
}
