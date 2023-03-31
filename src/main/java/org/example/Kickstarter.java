package org.example;

import org.example.annotation.DependencyEnabler;
import org.example.annotation.Enable;
import org.example.annotation.Inject;
import org.example.annotation.DependencyInjector;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class Kickstarter {

    private static final Map<String, Object> enabledDependencies = new HashMap<>();
    private static final Map<Class<?>, Object> classesToInject = new HashMap<>();
    private static final ClassLoader classLoader = Kickstarter.class.getClassLoader();

    private Kickstarter() {

    }

    public static void ignite() throws ClassNotFoundException {
        String rootPackagePath = Kickstarter.class.getPackageName();
        ignite(rootPackagePath);
    }

    private static void ignite(String packagePath) throws ClassNotFoundException {
        List<Class<?>> packagePathList = getPackagepath(packagePath, new ArrayList<>());
        declareAnnotatedField(DependencyEnabler.class, Enable.class, packagePathList);
        initializeAnnotatedField(DependencyInjector.class, Inject.class, packagePathList);
    }

    private static void initializeAnnotatedField(Class<DependencyInjector> parent, Class<Inject> child, List<Class<?>> packagePathList) {
        packagePathList.stream().filter(foundClass -> foundClass.getAnnotation(parent) != null)
                .forEach(foundClass -> {
                    if (foundClass.isInterface())
                        return;
                    try {
                        classesToInject.put(foundClass, foundClass.getConstructor().newInstance());
                        initializeField(child, foundClass, classesToInject.get(foundClass));

                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static void declareAnnotatedField(Class<DependencyEnabler> parent, Class<Enable> child, List<Class<?>> packagePathList) {
        packagePathList.stream()
                .filter(foundClass -> foundClass.getAnnotation(parent) != null)
                .forEach(foundClass -> {
                    Arrays.stream(foundClass.getDeclaredFields())
                            .filter(field -> field.getAnnotation(child) != null)
                            .forEach(field -> declareFieldAndCollect(child, field));
                });
    }

    private static void initializeField(Class<Inject> child, Class<?> foundClass, Object classInstance) {
            for (Field field : foundClass.getDeclaredFields()) {
                if (field.getAnnotation(child) != null) {
                    field.setAccessible(true);
                    try {
                        field.set(classInstance, enabledDependencies.get(field.getName()));
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
    }

    private static void declareFieldAndCollect(Class<Enable> child, Field field) {
        if (field.getType().isInterface()) {
            if (!field.getAnnotation(child).fullClassName().equals(Object.class)) {
                Class<?> interfaceToClass = field.getAnnotation(child).fullClassName();
                try {
                    enabledDependencies.put(field.getName(), interfaceToClass.getConstructor().newInstance());
                } catch (InstantiationException | IllegalAccessException |
                         InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            try {
                enabledDependencies.put(field.getName(), field.getType().getConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException |
                     InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static List<Class<?>> getPackagepath(String startPackagePath, List<Class<?>> packagePathList) throws ClassNotFoundException {

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
        return (T) classesToInject.get(classFileName);
    }
}