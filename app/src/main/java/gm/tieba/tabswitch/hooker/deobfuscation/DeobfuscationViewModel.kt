package gm.tieba.tabswitch.hooker.deobfuscation

import android.content.Context
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class DeobfuscationViewModel {

    private val _progress = PublishSubject.create<Float>()
    val progress: Observable<Float> = _progress
    private val deobfuscation = Deobfuscation()

    fun deobfuscate(context: Context, matchers: List<Matcher>) {
        deobfuscation.setMatchers(matchers)
        deobfuscation.dexkit(_progress, context)
        deobfuscation.saveDexSignatureHashCode()
    }
}
