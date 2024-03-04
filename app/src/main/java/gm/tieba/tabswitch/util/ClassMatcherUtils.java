package gm.tieba.tabswitch.util;

import org.luckypray.dexkit.query.matchers.ClassMatcher;

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

    public ClassMatcher getMatcher() {
        return this.matcher;
    }
}
