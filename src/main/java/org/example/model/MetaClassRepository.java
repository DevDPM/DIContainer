package org.example.model;

import java.util.*;

// purpose is storing and retrieving objects
public class MetaClassRepository {

    private static final Map<Class<?>, Set<MetaClass>> injectables = new HashMap<>();

    public static Set<MetaClass> getMetaClassByClass(Class<?> className) {
        return injectables.get(className);
    }

    public static MetaClass getFirstMetaClassByClass(Class<?> baseClass) {
        return getFirstMetaClassByClass(baseClass, "");
    }

    public static MetaClass getFirstMetaClassByClass(Class<?> baseClass, String optionalName) {
        return injectables.get(baseClass)
                .stream()
                .filter(e -> {
                    if (optionalName.equals("")) {
                        return true;
                    }else {
                        System.out.println(e.getAdditionalInfo() + " vs " + optionalName);
                        return e.getAdditionalInfo().equals(optionalName);
                    }
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("not found"));
    }

    public static void saveMetaClassByClassType(Class<?> type, MetaClass metaClass) {
        if (injectables.get(type) != null) {
            Set<MetaClass> value = injectables.get(type);
            if (value.add(metaClass)) {
                injectables.put(type, value);
            }
        } else {
            injectables.put(type, new HashSet<>(List.of(metaClass)));
        }
    }

    public static Map<Class<?>, Set<MetaClass>> getAllInjectables() {
        return injectables;
    }
}
