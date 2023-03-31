package org.example;

import org.example.annotation.Globalization;
import org.example.annotation.Globalize;
import org.example.annotation.Inject;
import org.example.annotation.Pilot;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Kickstarter {

    private Kickstarter() {

    }

    private static Map<String, Object> dependencyHolder = new HashMap<>();
    private static Map<Class, Object> pilotHolder = new HashMap<>();

    public static void ignite() throws ClassNotFoundException {
        try {
            ClassLoader classLoader = Kickstarter.class.getClassLoader();

            String dottedPackagePath = Kickstarter.class.getPackageName();
            String packagePath = dottedPackagePath.replace('.', '/');
            URL url = classLoader.getResource(packagePath);

            if (url == null)
                return;

            File folder = new File(url.getPath());
            File[] content = folder.listFiles();

            for (int i = 0; i < content.length; i++) {
                int index = content[i].getName().indexOf(".");

                if (!content[i].getName().contains(".class"))
                    continue;

                String className = content[i].getName().substring(0, index);

                String classNamePath = dottedPackagePath + "." + className;
                Class<?> scanClass = Class.forName(classNamePath);

                if (scanClass.getAnnotation(Globalization.class) != null) {

                    for (Field field : scanClass.getDeclaredFields()) {
                        if (field.getAnnotation(Globalize.class) != null) {

                            if (field.getType().isInterface()) {
                                if (!field.getAnnotation(Globalize.class).fullClassName().equals(Object.class)) {
                                    Class<?> interfaceToClass = field.getAnnotation(Globalize.class).fullClassName();
                                    dependencyHolder.put(field.getName(), interfaceToClass.getConstructor().newInstance());
                                }
                            } else {
                                dependencyHolder.put(field.getName(), field.getType().getConstructor().newInstance());
                            }
                        }
                    }
                } else if (scanClass.getAnnotation(Pilot.class) != null) {
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