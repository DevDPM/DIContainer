package org.dpmFramework.model;

import org.dpmFramework.Kickstarter;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class ClassURIRepository {

    private static Set<Class<?>> classes = new HashSet<>();
    private static Set<Class<?>> interfaces = new HashSet<>();
    private static ClassLoader classLoader;

    private ClassURIRepository() {
    }

    public static void init(Class<?> startClass) {
        classLoader = startClass.getClassLoader();
        String rootPackagePath = startClass.getPackageName();
        loadClassPaths(rootPackagePath);
    }

    private static void loadClassPaths(String packagePath) {
        String path = packagePath.replace('.', '/');
        URL url = classLoader.getResource(path);

        if (url == null)
            throw new RuntimeException("Null pointer; Could not get URL from ClassLoader");

        File folder = new File(url.getPath());
        File[] fileArray = folder.listFiles();

        if (fileArray == null)
            throw new RuntimeException("Null pointer; fileArray did not retrieve any files");

        separateClassAndInterface(packagePath, fileArray);
    }

    private static void separateClassAndInterface(String startPackagePath, File[] fileArray) {
        for (int i = 0; i < fileArray.length; i++) {
            String fileName = fileArray[i].getName();
            int index = fileName.indexOf(".");

            if (fileName.contains("$"))
                continue;

            if (!fileName.contains(".class")) {
                String childPath = startPackagePath + "." + fileName;
                loadClassPaths(childPath);
                continue;
            }

            String className = fileName.substring(0, index);
            String classNamePath = startPackagePath + "." + className;
            Class<?> foundClass;
            try {
                foundClass = Class.forName(classNamePath);
                if (foundClass.isInterface()) {
                    interfaces.add(foundClass);
                } else {
                    classes.add(foundClass);
                }
            } catch (ClassNotFoundException ex) {
                ex.getStackTrace();
            }
        }
    }

    public static Set<Class<?>> getClasses() {
        return classes;
    }

    public static Set<Class<?>> getInterfaces() {
        return interfaces;
    }
}
