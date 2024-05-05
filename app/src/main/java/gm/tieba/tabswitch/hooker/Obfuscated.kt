package gm.tieba.tabswitch.hooker

import gm.tieba.tabswitch.hooker.deobfuscation.Matcher

interface Obfuscated {

    fun matchers(): List<Matcher>
}
