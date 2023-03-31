package org.example;

import org.example.annotation.Configuration;
import org.example.annotation.Enable;
import org.example.annotation.Inject;
import org.example.annotation.Service;
import org.example.model.BigClass;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class Kickstarter {

    private static final Map<Class<?>, Set<BigClass>> injectables = new HashMap<>();
    private static final ClassLoader classLoader = Kickstarter.class.getClassLoader();

    private Kickstarter() {

    }

    public static void ignite() throws ClassNotFoundException {
        String rootPackagePath = Kickstarter.class.getPackageName();
        ignite(rootPackagePath);
    }

    private static void ignite(String packagePath) throws ClassNotFoundException {
        List<Class<?>> packagePathList = getClassTree(packagePath, new ArrayList<>());
        Set<Class<?>> interfaces = new HashSet<>();

        // instantiate all components into bigClasses as bigClass
        packagePathList.forEach(baseClass -> {
            if (!validateClass(baseClass))
                return;

            if (baseClass.isInterface()) {
                interfaces.add(baseClass);
                return;
            }

            System.out.println(baseClass);

            if (baseClass.isAnnotationPresent(Configuration.class)) {

                // check for @Enable and initialize them
                for (Field field : baseClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Enable.class)) {
                        BigClass bigClass = new BigClass();
                        bigClass.setObservedClass(field.getType());
                        bigClass.setInstance(new HashSet<>(Collections.singletonList(createInstance(field.getType()))));

                        bigClass.setAdditionalInfo(field.getAnnotation(Enable.class).name());
                        if (baseClass.getInterfaces().length > 0)
                            bigClass.setImplementations(new HashSet<>(List.of(baseClass.getInterfaces())));

                        injectables.put(field.getType(), new HashSet<>(Set.of(bigClass)));
                    }
                }
            } else if (baseClass.isAnnotationPresent(Service.class)) {
                BigClass bigClass = new BigClass();
                bigClass.setObservedClass(baseClass);
                bigClass.setInstance(new HashSet<>(Collections.singletonList(createInstance(baseClass))));
                bigClass.setAdditionalInfo(baseClass.getAnnotation(Service.class).name());
                if (baseClass.getInterfaces().length > 0)
                    bigClass.setImplementations(new HashSet<>(List.of(baseClass.getInterfaces())));

                injectables.put(baseClass, new HashSet<>(Set.of(bigClass)));
            }
        });


        // check if interfaces were encountered -> solve it
        if (interfaces.size() > 0) {

            interfaces.forEach(baseIFClass -> {
                injectables.entrySet().forEach(e -> {
                    for (BigClass bigClass : e.getValue()) {
                        bigClass.getImplementations().forEach(implementation -> {
                            if (implementation.getName().equals(baseIFClass.getName())) {
                                injectables.computeIfPresent(baseIFClass, (k, v) -> v).add(bigClass);
                                injectables.putIfAbsent(baseIFClass, new HashSet<>(List.of(bigClass)));
                            }
                        });
                    }
                });
            });
        }

        packagePathList.forEach(baseClass -> {

            if (!validateClass(baseClass) || baseClass.isInterface() || !baseClass.isAnnotation())
                return;


            Optional<Set<BigClass>> classContent = injectables.entrySet()
                    .stream()
                    .filter(e -> e.getKey().getName().equals(baseClass.getName()))
                    .map(Map.Entry::getValue)
                    .findFirst();


            BigClass baseClassInstance = classContent.get()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("could not find baseClass"));


            for (Field field : baseClass.getDeclaredFields()){
                if (field.isAnnotationPresent(Inject.class)) {

                    Set<BigClass> fieldInstances = injectables.get(field.getType());

                    if (fieldInstances == null) {
                        System.out.println("Field is not @Enable for wiring: " + field.getName());
                        return;
                    }

                    Class<?> annotatedClassName = field.getAnnotation(Inject.class).className();
                    String annotatedStringName = field.getAnnotation(Inject.class).name();

                    if (annotatedClassName.equals(Object.class) && annotatedStringName.equals("")) {
                        BigClass fieldInstance = fieldInstances.stream()
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Did not find anything at field: " + field.getName()));
                        setField(field, baseClassInstance, fieldInstance);

                    } else if (!annotatedStringName.equals("")) {
                        BigClass fieldInstance = fieldInstances.stream()
                                .filter(e -> e.getAdditionalInfo().equals(annotatedStringName))
                                .findFirst().orElseThrow(() -> new RuntimeException("Did not find a match naming in field: " + field.getName()));
                        setField(field, baseClassInstance, fieldInstance);

                    } else {

                        BigClass fieldInstance = fieldInstances.stream()
                                .filter(e -> e.getObservedClass().getName().equals(annotatedClassName.getName()))
                                .findFirst().orElseThrow(() -> new RuntimeException("Did not find a match naming in field: " + field.getName()));
                        setField(field, baseClassInstance, fieldInstance);
                    }
                }
            }

        });

    }

    private static void setField(Field field, BigClass baseClassInstance, BigClass fieldInstance) {
        field.setAccessible(true);

        try {
            field.set(baseClassInstance, fieldInstance);
        } catch (IllegalAccessException e) {
            e.getMessage();
        }

    }

    private static boolean validateClass(Class<?> baseClass) {
        if (baseClass.isEnum() || baseClass.isRecord())
            return false;
        return true;
    }

    private static <T> T createInstance(Class<T> type)  {
        try {
            return type.getConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            System.out.println("OBJECT COULD NOT NEWLY BE INSTANTIATED: " + type.getName());
            ex.getMessage();
        }
        return null;
    }

    private static List<Class<?>> getClassTree(String startPackagePath, List<Class<?>> packagePathList) throws ClassNotFoundException {

        String path = startPackagePath.replace('.', '/');
        URL url = classLoader.getResource(path);

        if (url == null)
            return new ArrayList<>();

        File folder = new File(url.getPath());
        File[] fileArray = folder.listFiles();

        for (int i = 0; i < fileArray.length; i++) {
            String fileName = fileArray[i].getName();
            int index = fileName.indexOf(".");

            if (fileName.contains("$"))
                continue;

            if (!fileName.contains(".class")) {
                String childPath = startPackagePath + "." + fileName;
                ignite(childPath);
                continue;
            }

            String className = fileName.substring(0, index);

            String classNamePath = startPackagePath + "." + className;
            Class<?> foundClass = Class.forName(classNamePath);
            packagePathList.add(foundClass);
        }
        return packagePathList;
    }

    public static <T> T getInstanceOf(Class<T> classFileName) {
        return (T) injectables.get(classFileName);
    }
}