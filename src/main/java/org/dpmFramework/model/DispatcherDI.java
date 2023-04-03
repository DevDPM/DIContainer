package org.dpmFramework.model;

import org.dpmFramework.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

// delegates the metaClass service of what needs to be done
public class DispatcherDI {

    public static void bootstrapClassPaths(Class<?> startClass) {
        ClassURIService.bootstrapRepository(startClass);
    }

    public static void instantiateClassesByAnnotation() {

        if (ClassURIService.getAllClasses().isEmpty())
            throw new RuntimeException("Cannot retrieve all classes");

        ClassURIService.getAllClasses().forEach(baseClass -> {
            if (ClassURIService.isNotPlainClassOrInterface(baseClass))
                return;

            if (baseClass.isAnnotationPresent(Configurations.class)) {
                for (Field field : baseClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Enable.class)) {
                        Class<?> fieldClass = field.getType();
                        MetaClass metaClass = MetaClassService.createMetaClass(baseClass, fieldClass);
                        metaClass.setAdditionalInfo(field.getAnnotation(Enable.class).name());
                        MetaClassRepository.saveMetaClassByClassType(field.getType(), metaClass);
                        AnnotatedClassRepository.addConfigurationsClass(baseClass);
                    }
                }
            } else if (baseClass.isAnnotationPresent(Service.class)) {
                MetaClass metaClass = MetaClassService.createMetaClass(baseClass);
                metaClass.setAdditionalInfo(baseClass.getAnnotation(Service.class).name());
                MetaClassRepository.saveMetaClassByClassType(baseClass, metaClass);
                AnnotatedClassRepository.addServiceClass(baseClass);

            } else if (baseClass.isAnnotationPresent(Controller.class)) {
                System.out.println(baseClass + " is controller");
                MetaClass metaClass = MetaClassService.createMetaClass(baseClass);
//                metaClass.setAdditionalInfo(baseClass.getAnnotation(Controller.class).name());
                MetaClassRepository.saveMetaClassByClassType(baseClass, metaClass);
                AnnotatedClassRepository.addControllerClass(baseClass);
            }
        });
    }

    public static void wireInterfaceClassToImplementedMetaClass() {
        List<MetaClass[]> temp = new ArrayList<>();
        for (Class<?> interfaceClass : ClassURIService.getAllInterfaces()) {
            MetaClassService.getAllInjectables().forEach((key, metaClassSet) -> metaClassSet.forEach(metaClass -> {
                if (metaClass.getImplementations() != null) {
                    metaClass.getImplementations().forEach(implementation -> {
                        if (implementation.getName().equals(interfaceClass.getName())) {
                            MetaClass metaInterfaceClass = new MetaClass();
                            metaInterfaceClass.setObservedClass(interfaceClass);

                            MetaClass[] holder = new MetaClass[2];
                            holder[0] = metaInterfaceClass;
                            holder[1] = metaClass;
                            temp.add(holder);
                        }
                    });
                }
            }));
        }
        temp.forEach(e -> {
            MetaClassRepository.saveMetaClassByClassType(e[0].getObservedClass(), e[1]);
        });
    }

    public static void initializeAllFields() {
        ClassURIService.getAllClasses().forEach(parentClass -> {
            if (ClassURIService.isNotPlainClassOrInterface(parentClass) ||
                    parentClass.isInterface() ||
                    (!parentClass.isAnnotationPresent(Service.class) &&
                    !parentClass.isAnnotationPresent(Controller.class))) {
                return;
            }
            MetaClass parentClassInstance = MetaClassRepository.getMetaClassByClass(parentClass);

            for (Field classField : parentClass.getDeclaredFields()){
                if (classField.isAnnotationPresent(Inject.class)) {
                    Set<MetaClass> fieldInstances = MetaClassRepository.getMetaClassesByClass(classField.getType());
                    if (fieldInstances == null) {
                        System.out.println("Field is not @Enable for wiring: " + classField.getName());
                        return;
                    }
                    MetaClass fieldMetaClass = unpackMetaClass(classField, fieldInstances);
                    initializeField(classField, parentClassInstance, fieldMetaClass);
                }
            }
        });
    }

    private static MetaClass unpackMetaClass(Field field, Set<MetaClass> fieldInstances) {
        Class<?> annotatedClassName = field.getAnnotation(Inject.class).className();
        String annotatedStringName = field.getAnnotation(Inject.class).name();

        if (annotatedClassName.equals(Object.class) && annotatedStringName.equals("")) {
            return fieldInstances.stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Did not find anything at field: " + field.getName()));

        } else if (!annotatedStringName.equals("")) {
            return fieldInstances.stream()
                    .filter(e -> e.getAdditionalInfo().equals(annotatedStringName))
                    .findFirst().orElseThrow(() -> new RuntimeException("Did not find a match naming in field: " + field.getName()));
        } else {
            return fieldInstances.stream()
                    .filter(e -> e.getObservedClass().getName().equals(annotatedClassName.getName()))
                    .findFirst().orElseThrow(() -> new RuntimeException("Did not find a match naming in field: " + field.getName()));
        }
    }

    private static void initializeField(Field field, MetaClass baseClass, MetaClass fieldClass) {
        field.setAccessible(true);
        Object baseClassInstance = baseClass.getInstance();
        Object fieldInstance = fieldClass.getInstance();

        try {
            field.set(baseClassInstance, fieldInstance);
        } catch (IllegalAccessException e) {
            e.getMessage();
        }
    }

    public static void callInit() {
        if (AnnotatedClassRepository.getControllers().isEmpty())
            return;

        AnnotatedClassRepository.getControllers().forEach(controllerClass -> {
            System.out.println(controllerClass);
            try {
                for (Method method : controllerClass.getDeclaredMethods()) {
                    if (method.getName().equals("init")) {
                        method.setAccessible(true);
                        method.invoke(MetaClassRepository.getMetaClassByClass(controllerClass).getInstance());
                    }
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
