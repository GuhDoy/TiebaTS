package gm.tieba.tabswitch.hooker.deobfuscation

import org.luckypray.dexkit.query.matchers.ClassMatcher

abstract class Matcher {
    abstract override fun toString(): String
}

class StringMatcher(val str: String) : Matcher() {
    override fun toString(): String = str
}

class SmaliMatcher(val str: String) : Matcher() {
    override fun toString(): String = str
}

open class ResMatcher(var id: Long = 0) : Matcher() {
    override fun toString(): String {
        throw UnsupportedOperationException()
    }
}

class StringResMatcher(val str: String) : ResMatcher() {
    override fun toString(): String = str
}

class ZipEntryMatcher(val size: Long) : ResMatcher() {
    var entryName: String = ""
    override fun toString(): String = size.toString()
}

class ResIdentifierMatcher(val name: String, val defType: String) : ResMatcher() {
    override fun toString(): String = String.format("%s.%s", defType, name)
}

class PackageMatcher(val classMatcher: ClassMatcher, val classMatcherIdentifier: String, val subMatcher: Matcher) : Matcher() {
    override fun toString(): String = String.format("%s/%s", classMatcherIdentifier, subMatcher.toString())
}

class MethodNameMatcher(val name: String) : Matcher() {
    override fun toString(): String = name
}