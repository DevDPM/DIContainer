package org.dpmFramework.model;

import java.util.*;

// purpose is storing and retrieving objects
public class MetaClassRepository {

    private static final Map<Class<?>, Set<MetaClass>> injectables = new HashMap<>();

    public static Set<MetaClass> getMetaClassesByClass(Class<?> className) {
        return injectables.get(className);
    }

    public static MetaClass getMetaClassByClass(Class<?> baseClass) {
        return getMetaClassesByClass(baseClass, "");
    }

    public static MetaClass getMetaClassesByClass(Class<?> baseClass, String optionalName) {
        return injectables.get(baseClass)
                .stream()
                .filter(e -> {
                    if (optionalName.equals("")) {
                        return true;
                    }else {
//                        System.out.println(e.getAdditionalInfo() + " vs " + optionalName);
                        return e.getAdditionalInfo().equals(optionalName);
                    }
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("not found"));
    }

    public static void saveOrUpdateMetaClassByClassKey(Class<?> classKey, MetaClass metaClass) {
        if (injectables.get(classKey) != null) {
            Set<MetaClass> value = injectables.get(classKey);
            if (value.add(metaClass)) {
                injectables.put(classKey, value);
            }
        } else {
            injectables.put(classKey, new HashSet<>(List.of(metaClass)));
        }
    }

    public static Map<Class<?>, Set<MetaClass>> getAllInjectables() {
        return injectables;
    }
}
