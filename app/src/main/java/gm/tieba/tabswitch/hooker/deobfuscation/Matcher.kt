package gm.tieba.tabswitch.hooker.deobfuscation

import org.luckypray.dexkit.query.matchers.ClassMatcher

class ClassMatcherHelper(val matcher: ClassMatcher, val id: String) {
    override fun toString(): String = "$id/"

    companion object {
        @JvmStatic
        fun usingString(str: String): ClassMatcherHelper {
            val matcher = ClassMatcher.create().usingStrings(str)
            return ClassMatcherHelper(matcher, str)
        }
    }
}

abstract class Matcher(val classMatcher: ClassMatcherHelper? = null) {
    override fun toString(): String = classMatcher?.toString().orEmpty()
}

class StringMatcher @JvmOverloads constructor(val str: String, classMatcher: ClassMatcherHelper? = null) : Matcher(classMatcher) {
    override fun toString(): String = super.toString() + str
}

class SmaliMatcher @JvmOverloads constructor(val str: String, classMatcher: ClassMatcherHelper? = null) : Matcher(classMatcher) {
    override fun toString(): String = super.toString() + str
    fun getDescriptor(): String = str;
}

class MethodNameMatcher @JvmOverloads constructor(val name: String, classMatcher: ClassMatcherHelper? = null) : Matcher(classMatcher) {
    override fun toString(): String = super.toString() + name
}

open class ResMatcher @JvmOverloads constructor(var id: Long = 0, classMatcher: ClassMatcherHelper? = null) : Matcher(classMatcher) {
    open fun toResIdentifier(): String {
        throw UnsupportedOperationException()
    }
}

class StringResMatcher @JvmOverloads constructor(val str: String, classMatcher: ClassMatcherHelper? = null) : ResMatcher(classMatcher = classMatcher) {
    override fun toString(): String = super.toString() + str
    override fun toResIdentifier(): String = str
}

class ZipEntryMatcher @JvmOverloads constructor(val size: Long, classMatcher: ClassMatcherHelper? = null) : ResMatcher(classMatcher = classMatcher) {
    var entryName: String = ""
    override fun toString(): String = super.toString() + size.toString()
    override fun toResIdentifier(): String = size.toString()
}

class ResIdentifierMatcher @JvmOverloads constructor(val name: String, val defType: String, classMatcher: ClassMatcherHelper? = null) : ResMatcher(classMatcher = classMatcher) {
    override fun toString(): String = super.toString() + String.format("%s.%s", defType, name)
    override fun toResIdentifier(): String = String.format("%s.%s", defType, name)
}