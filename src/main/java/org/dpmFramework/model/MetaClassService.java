package org.dpmFramework.model;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// purpose is provide services (manipulate) for MetaClass
public class MetaClassService {

    public static MetaClass createMetaClass(Class<?> baseClass) {
        return createMetaClass(baseClass, baseClass);
    }

    public static MetaClass createMetaClass(Class<?> baseClass, Class<?> observedClass) {
        MetaClass metaClass = new MetaClass();
        metaClass.setObservedClass(observedClass);
        metaClass.setInstance(Objects.requireNonNull(createInstance(observedClass)));
        if (baseClass.getInterfaces().length > 0) {
            metaClass.setImplementations(new HashSet<>(List.of(baseClass.getInterfaces())));
        }
        return metaClass;
    }

    private static <T> T createInstance(Class<T> type)  {
        System.out.println("instance "+type);
        try {
            return type.getConstructor(new Class<?> []{}).newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            System.out.println("OBJECT COULD NOT NEWLY BE INSTANTIATED: " + type.getName());
            ex.getStackTrace();
        }
        return null;
    }

    public static Map<Class<?>, Set<MetaClass>> getAllInjectables() {
        return MetaClassRepository.getAllInjectables();
    }

    public static void printDIManagementList() {
        System.out.println("_____ DI Injectables list");
        AtomicInteger counter = new AtomicInteger(0);
        MetaClassRepository.getAllInjectables().entrySet().forEach(a -> {
            System.out.println("__________");
            System.out.println(counter.incrementAndGet() + ". Class/Field -> "+a.getKey().getSimpleName());
            a.getValue().forEach(b -> {
                System.out.println("\tInstance:\t\t\t" + b.getInstance());
                System.out.println("\tObservedClass:\t\t" + b.getObservedClass());
                System.out.println("\t@Enable (name = ?):\t" + b.getAdditionalInfo());
                System.out.println();
            });
        });
    }
}