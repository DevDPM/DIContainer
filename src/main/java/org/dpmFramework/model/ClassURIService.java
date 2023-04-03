package org.dpmFramework.model;

import java.util.Set;

public class ClassURIService {

    public static Set<Class<?>> getAllClasses() {
        return ClassURIRepository.getClasses();
    }

    public static Set<Class<?>> getAllInterfaces() {
        return ClassURIRepository.getInterfaces();
    }

    public static boolean isNotPlainClassOrInterface(Class<?> baseClass) {
        if (baseClass.isEnum() || baseClass.isRecord() || baseClass.isAnnotation() || baseClass.isAnonymousClass()
                || baseClass.isMemberClass())
            return true;
        return false;
    }

    public static void bootstrapRepository(Class<?> startClass) {
        if (ClassURIRepository.getClasses().isEmpty()) {
            ClassURIRepository.init(startClass);
        }
    }
}
