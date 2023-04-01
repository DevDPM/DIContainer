package org.example.model;

import org.example.annotation.Configurations;
import org.example.annotation.Enable;
import org.example.annotation.Inject;
import org.example.annotation.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                    }
                }
            } else if (baseClass.isAnnotationPresent(Service.class)) {
                MetaClass metaClass = MetaClassService.createMetaClass(baseClass);
                metaClass.setAdditionalInfo(baseClass.getAnnotation(Service.class).name());
                MetaClassRepository.saveMetaClassByClassType(baseClass, metaClass);
            }
        });
    }

    public static void wireInterfaceClassToImplementedMetaClass() {
        List<MetaClass[]> temp = new ArrayList<>();
        for (Class<?> baseIFClass : ClassURIService.getAllInterfaces()) {
            for (Map.Entry<Class<?>, Set<MetaClass>> entity : MetaClassService.getAllInjectables().entrySet()) {
                for (MetaClass metaClass : entity.getValue()) {
                    if (metaClass.getImplementations() == null)
                        continue;

                    for (Class<?> IFClass : metaClass.getImplementations()) {
                        if (IFClass.getName().equals(baseIFClass.getName())) {
                            MetaClass interfaceClass = new MetaClass();
                            interfaceClass.setObservedClass(baseIFClass);

                            MetaClass[] holder = new MetaClass[2];
                            holder[0] = interfaceClass;
                            holder[1] = metaClass;
                            temp.add(holder);
                        }
                    }
                }
            }
        }
        temp.forEach(e -> {
            MetaClassRepository.saveMetaClassByClassType(e[0].getObservedClass(), e[1]);
        });
    }

    public static void initializeClasses() {
        ClassURIService.getAllClasses().forEach(baseClass -> {
            if (ClassURIService.isNotPlainClassOrInterface(baseClass) || baseClass.isInterface() || !baseClass.isAnnotationPresent(Service.class)) {
                return;
            }

            MetaClass baseClassInstance = MetaClassRepository.getFirstMetaClassByClass(baseClass);

            for (Field field : baseClass.getDeclaredFields()){
                if (field.isAnnotationPresent(Inject.class)) {
                    Set<MetaClass> fieldInstances = MetaClassRepository.getMetaClassByClass(field.getType());
                    if (fieldInstances == null) {
                        System.out.println("Field is not @Enable for wiring: " + field.getName());
                        return;
                    }
                    initializeField(baseClassInstance, field, fieldInstances);
                }
            }
        });
    }

    private static void initializeField(MetaClass baseClassInstance, Field field, Set<MetaClass> fieldInstances) {
        Class<?> annotatedClassName = field.getAnnotation(Inject.class).className();
        String annotatedStringName = field.getAnnotation(Inject.class).name();

        if (annotatedClassName.equals(Object.class) && annotatedStringName.equals("")) {
            MetaClass fieldInstance = fieldInstances.stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Did not find anything at field: " + field.getName()));

            initializeField(field, baseClassInstance, fieldInstance);
        } else if (!annotatedStringName.equals("")) {
            MetaClass fieldInstance = fieldInstances.stream()
                    .filter(e -> e.getAdditionalInfo().equals(annotatedStringName))
                    .findFirst().orElseThrow(() -> new RuntimeException("Did not find a match naming in field: " + field.getName()));
            initializeField(field, baseClassInstance, fieldInstance);

        } else {
            MetaClass fieldInstance = fieldInstances.stream()
                    .filter(e -> e.getObservedClass().getName().equals(annotatedClassName.getName()))
                    .findFirst().orElseThrow(() -> new RuntimeException("Did not find a match naming in field: " + field.getName()));
            initializeField(field, baseClassInstance, fieldInstance);
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

}
