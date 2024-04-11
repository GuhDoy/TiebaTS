package gm.tieba.tabswitch.hooker.deobfuscation

import gm.tieba.tabswitch.util.ClassMatcherUtils

class MatcherProperties {
    var classMatcher: ClassMatcherUtils? = null
    var requiredVersion: String? = null
    companion object {
        @JvmStatic
        fun create() : MatcherProperties {
            return MatcherProperties()
        }
    }
    override fun toString(): String {
        val versionString = requiredVersion?.let { "$it@" } ?: ""
        return versionString + classMatcher?.toString().orEmpty()
    }
    fun useClassMatcher(classMatcher: ClassMatcherUtils) : MatcherProperties {
        this.classMatcher = classMatcher
        return this
    }
    fun requireVersion(requiredVersion: String) : MatcherProperties {
        this.requiredVersion = requiredVersion
        return this
    }
}

abstract class Matcher(private val properties: MatcherProperties? = null) {
    override fun toString(): String = properties?.toString().orEmpty()
    fun getClassMatcher(): ClassMatcherUtils? = properties?.classMatcher
    fun getRequiredVersion(): String? = properties?.requiredVersion

}

class StringMatcher @JvmOverloads constructor(val str: String, properties: MatcherProperties? = null) : Matcher(properties) {
    override fun toString(): String = super.toString() + str
}

class SmaliMatcher @JvmOverloads constructor(val str: String, properties: MatcherProperties? = null) : Matcher(properties) {
    override fun toString(): String = super.toString() + str
    fun getDescriptor(): String = str;
}

class MethodNameMatcher @JvmOverloads constructor(val name: String, properties: MatcherProperties? = null) : Matcher(properties) {
    override fun toString(): String = super.toString() + name
}

class ReturnTypeMatcher<T> @JvmOverloads constructor(val returnType: Class<T>, properties: MatcherProperties? = null) : Matcher(properties) {
    override fun toString(): String = super.toString() + returnType.simpleName
}

open class ResMatcher @JvmOverloads constructor(var id: Long = 0, properties: MatcherProperties? = null) : Matcher(properties) {
    open fun toResIdentifier(): String {
        throw UnsupportedOperationException()
    }
}

class StringResMatcher @JvmOverloads constructor(val str: String, properties: MatcherProperties? = null) : ResMatcher(properties = properties) {
    override fun toString(): String = super.toString() + str
    override fun toResIdentifier(): String = str
}

class ResIdentifierMatcher @JvmOverloads constructor(val name: String, val defType: String, properties: MatcherProperties? = null) : ResMatcher(properties = properties) {
    override fun toString(): String = super.toString() + String.format("%s.%s", defType, name)
    override fun toResIdentifier(): String = String.format("%s.%s", defType, name)
}