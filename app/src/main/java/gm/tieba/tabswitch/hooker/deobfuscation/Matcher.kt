package gm.tieba.tabswitch.hooker.deobfuscation

import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher

abstract class Matcher(private val name: String) {
    var classMatcher: ClassMatcher? = null
    var reqVersion: String? = null
    abstract val methodMatcher: MethodMatcher

    override fun toString(): String = name
}

class StringMatcher (str: String, name: String = str) : Matcher(name) {
    override val methodMatcher = MethodMatcher.create().usingStrings(str)
}

class SmaliMatcher (descriptor: String, name: String = descriptor) : Matcher(name) {
    override val methodMatcher = MethodMatcher.create().addInvoke(MethodMatcher.create().descriptor(descriptor))
}

class MethodNameMatcher(methodName: String, name: String) : Matcher(name) {
    override val methodMatcher = MethodMatcher.create().name(methodName)
}

class ReturnTypeMatcher<T>(returnType: Class<T>, name: String) : Matcher(name) {
    override val methodMatcher = MethodMatcher.create().returnType(returnType)
}

class ResMatcher(id: Long, name: String) : Matcher(name) {
    override val methodMatcher = MethodMatcher.create().usingNumbers(id)
}