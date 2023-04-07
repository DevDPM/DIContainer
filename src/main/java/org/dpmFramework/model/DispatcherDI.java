package org.dpmFramework.model;

import org.dpmFramework.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

// delegates the metaClass service of what needs to be done
public class DispatcherDI {

    public static void bootstrapClassPaths(Class<?> startClass) {
        ClassService.bootstrapRepository(startClass);
    }

    public static void instantiateClassesByAnnotation() {

        if (ClassService.getAllClasses().isEmpty())
            throw new RuntimeException("Cannot retrieve all classes");

        // get all bootstrapped Classes
        ClassService.getAllClasses().forEach(topClass -> {
            if (ClassService.isNotPlainClassOrInterface(topClass))
                return;

            // search for class @Configuration
            if (topClass.isAnnotationPresent(Configurations.class)) {
                for (Field field : topClass.getDeclaredFields()) {
                    // search for field @Enable in @Configuration class
                    if (field.isAnnotationPresent(Enable.class)) {
                        Class<?> fieldClass = field.getType(); // get Class object from annotated field
                        MetaClass metaClass = MetaClassService.createMetaClass(topClass, fieldClass); // create a class containing meta information (MetaClass)
                        metaClass.setAdditionalInfo(field.getAnnotation(Enable.class).name()); // add additional information i.e @Enable (name = "additional_Info")
                        MetaClassRepository.saveOrUpdateMetaClassByClassKey(fieldClass, metaClass); // save into Map (key: field Class - value: it's metaClass)
                        AnnotatedClassRepository.addConfigurationsClass(topClass); // added in version 2.0 to separate annotated classes from each other for diff. purpose
                    }
                }
                // repeat for @Service
            } else if (topClass.isAnnotationPresent(Service.class)) {
                MetaClass metaClass = MetaClassService.createMetaClass(topClass);
                metaClass.setAdditionalInfo(topClass.getAnnotation(Service.class).name());
                MetaClassRepository.saveOrUpdateMetaClassByClassKey(topClass, metaClass);
                AnnotatedClassRepository.addServiceClass(topClass);

                // repeat for @Controller
            } else if (topClass.isAnnotationPresent(Controller.class)) {
                MetaClass metaClass = MetaClassService.createMetaClass(topClass);
                metaClass.setAdditionalInfo(topClass.getAnnotation(Controller.class).name());
                MetaClassRepository.saveOrUpdateMetaClassByClassKey(topClass, metaClass);
                AnnotatedClassRepository.addControllerClass(topClass);
            }
        });
    }

    /*
     * Wires all interface classes to metaclasses containing the interface implementation
     * i.e. public ClassExample implements InterfaceExample {}
     * The following code iterates through all found interface classes.
     * And search for matching metaClasses containing identical implementation from interfaces.
     * If a match is found, The interface class and MetaClass that implements the interface class are put in a temporary holder.
     * After all iterations, all temp's will be wired and saved as new key: Interface class, value: Metaclass implementing the Interface class
     * */
    public static void wireInterfaceClassToImplementedMetaClass() {
        List<Object[]> temp = new ArrayList<>();
        for (Class<?> interfaceClass : ClassService.getAllInterfaces()) {
            MetaClassService.getAllInjectables().forEach((key, metaClassSet) -> {
                metaClassSet.forEach(metaClass -> {
                    if (metaClass.getImplementations() != null) {
                        metaClass.getImplementations().forEach(implementation -> {
                            if (implementation.equals(interfaceClass)) {

                                // store 2 different, but to-be-wired objects in 1 temporary array as Object
                                Object[] holder = new Object[2];
                                holder[0] = interfaceClass;
                                holder[1] = metaClass;
                                temp.add(holder);
                            }
                        });
                    }
                });
            });
        }

        temp.forEach(e -> {
            MetaClassRepository.saveOrUpdateMetaClassByClassKey((Class<?>) e[0], (MetaClass) e[1]);
        });
    }

    public static void initializeAllFields() {
        ClassService.getAllClasses().forEach(parentClass -> {
            if (ClassService.isNotPlainClassOrInterface(parentClass) ||
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
        callInitFromClasses(AnnotatedClassRepository.getControllers());

        if (AnnotatedClassRepository.getServices().isEmpty())
            return;
        callInitFromClasses(AnnotatedClassRepository.getServices());

    }

    public static void callInitFromClasses(Set<Class<?>> classes) {
        classes.forEach(controllerClass -> {
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
