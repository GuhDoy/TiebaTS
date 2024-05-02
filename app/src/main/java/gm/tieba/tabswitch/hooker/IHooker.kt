package gm.tieba.tabswitch.hooker;

import androidx.annotation.NonNull;

public interface IHooker {

    @NonNull
    String key();

    void hook() throws Throwable;
}
