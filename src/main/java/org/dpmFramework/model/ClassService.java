package org.dpmFramework.model;

import java.util.Set;

/*
* ClassService only provides services for the RepositoryURIClass.
* */

public class ClassService {

    public static Set<Class<?>> getAllClasses() {
        return ClassRepository.getClasses();
    }

    public static Set<Class<?>> getAllInterfaces() {
        return ClassRepository.getInterfaces();
    }

    public static boolean isNotPlainClassOrInterface(Class<?> baseClass) {
        if (baseClass.isEnum() || baseClass.isRecord() || baseClass.isAnnotation() || baseClass.isAnonymousClass()
                || baseClass.isMemberClass())
            return true;
        return false;
    }

    public static void bootstrapRepository(Class<?> startClass) {
        if (ClassRepository.getClasses().isEmpty()) {
            ClassRepository.bootstrap(startClass);
        }
    }
}
