package gm.tieba.tabswitch.util;

import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.MethodsMatcher;

public class ClassMatcherUtils {
    private final ClassMatcher matcher;
    private final String id;
    public ClassMatcherUtils(ClassMatcher matcher, String id) {
        this.matcher = matcher;
        this.id = id;
    }

    @Override
    public String toString() {
        return id + "/";
    }

    public static ClassMatcherUtils usingString(String str) {
        ClassMatcher classMatcher = ClassMatcher.create().usingStrings(str);
        return new ClassMatcherUtils(classMatcher, str);
    }

    public static ClassMatcherUtils className(String className) {
        ClassMatcher classMatcher = ClassMatcher.create().className(className);
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        return new ClassMatcherUtils(classMatcher, simpleClassName);
    }

    public static ClassMatcherUtils invokeMethod(String smali) {
        MethodMatcher invokeMatcher = MethodMatcher.create().addInvoke(
                MethodMatcher.create().descriptor(smali)
        );
        ClassMatcher classMatcher = ClassMatcher.create().methods(MethodsMatcher.create().add(invokeMatcher));
        return new ClassMatcherUtils(classMatcher, smali);
    }

    public ClassMatcher getMatcher() {
        return this.matcher;
    }
}
