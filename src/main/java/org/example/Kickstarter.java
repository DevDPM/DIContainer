package org.example;

import org.example.annotation.DependencyEnabler;
import org.example.annotation.Enable;
import org.example.annotation.Inject;
import org.example.annotation.DependencyInjector;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Kickstarter {

    private static Map<String, Object> dependencyHolder = new HashMap<>();
    private static Map<Class, Object> pilotHolder = new HashMap<>();


    private Kickstarter() {

    }

    public static void ignite() throws ClassNotFoundException {
        String rootPackagePath = Kickstarter.class.getPackageName();
        ignite(rootPackagePath);
    }

    private static void ignite(String packagePath) throws ClassNotFoundException {
        try {
            ClassLoader classLoader = Kickstarter.class.getClassLoader();
            String path = packagePath.replace('.', '/');

            URL url = classLoader.getResource(path);

            if (url == null)
                return;

            File folder = new File(url.getPath());
            File[] fileArray = folder.listFiles();

            for (int i = 0; i < fileArray.length; i++) {
                String fileName = fileArray[i].getName();
                int index = fileName.indexOf(".");

                if (fileName.contains("$"))
                    continue;

                if (!fileName.contains(".class")) {
                    String childPath = packagePath + "." + fileName;
                    ignite(childPath);
                    continue;
                }

                String className = fileName.substring(0, index);

                String classNamePath = packagePath + "." + className;
                Class<?> scanClass = Class.forName(classNamePath);

                if (scanClass.getAnnotation(DependencyEnabler.class) != null) {

                    for (Field field : scanClass.getDeclaredFields()) {
                        if (field.getAnnotation(Enable.class) != null) {

                            if (field.getType().isInterface()) {
                                if (!field.getAnnotation(Enable.class).fullClassName().equals(Object.class)) {
                                    Class<?> interfaceToClass = field.getAnnotation(Enable.class).fullClassName();
                                    dependencyHolder.put(field.getName(), interfaceToClass.getConstructor().newInstance());
                                }
                            } else {
                                dependencyHolder.put(field.getName(), field.getType().getConstructor().newInstance());
                            }
                        }
                    }
                } else if (scanClass.getAnnotation(DependencyInjector.class) != null) {
                    if (scanClass.isInterface())
                        return;
                    pilotHolder.put(scanClass, scanClass.getConstructor().newInstance());
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
        for (Map.Entry<Class, ?> globalClass : pilotHolder.entrySet()) {
            for (Field field : globalClass.getKey().getDeclaredFields()) {
                if (field.getAnnotation(Inject.class) != null) {
                    field.setAccessible(true);
                    try {
                        field.set(globalClass.getValue(), dependencyHolder.get(field.getName()));
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    public static <T> T returnType(Class<T> controller) {
        return (T) pilotHolder.get(controller);
    }
}