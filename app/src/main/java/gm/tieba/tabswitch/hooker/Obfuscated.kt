package gm.tieba.tabswitch.hooker;

import java.util.List;

import gm.tieba.tabswitch.hooker.deobfuscation.Matcher;

public interface Obfuscated {

    List<? extends Matcher> matchers();
}
