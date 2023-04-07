package org.dpmFramework.model;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class ClassRepository {

    private static Set<Class<?>> classes = new HashSet<>();
    private static Set<Class<?>> interfaces = new HashSet<>();
    private static ClassLoader classLoader;

    /*
    * ClassRepository ONLY holds all Classes and Interfaces individually.
    * Bootstrap is performed to find all classes and interfaces from 'startClass' path and higher.
    * */

    private ClassRepository() {
    }

    public static void bootstrap(Class<?> startClass) {
        classLoader = startClass.getClassLoader();
        String rootPackagePath = startClass.getPackageName(); // get package path of startClass
        loadClassPaths(rootPackagePath);
    }

    private static void loadClassPaths(String packagePath) {
        String path = packagePath.replace('.', '/');
        URL url = classLoader.getResource(path);

        if (url == null)
            throw new NullPointerException("Could not get URL from ClassLoader (= null)");

        File folder = new File(url.getPath());
        File[] fileArray = folder.listFiles(); // get array of file names (class or package) from within the root package path

        if (fileArray == null)
            throw new NullPointerException("FileArray did not retrieve any paths (= null)");

        convertPathToClass(packagePath, fileArray);
    }

    private static void convertPathToClass(String startPackagePath, File[] fileArray) {
        for (int i = 0; i < fileArray.length; i++) {
            String fileName = fileArray[i].getName();
            int index = fileName.indexOf("."); // get index of file name up until .class

            if (fileName.contains("$")) // check if filePath is innerClass (innerClass is ignored for dependency injection)
                continue;

            if (!fileName.contains(".class")) { // check if filepath is an .class file path, if not perform recursion
                String childPath = startPackagePath + "." + fileName;
                loadClassPaths(childPath); // recursion the none .class file path (only effective if package path)
                continue;
            }

            String className = fileName.substring(0, index); // subtract .class from filePath
            String classNamePath = startPackagePath + "." + className; // add package path + class name without .class
            Class<?> foundClass;
            try {
                foundClass = Class.forName(classNamePath); // return the class object from package class path
                if (foundClass.isInterface()) { // fill up repository with interfaces or classes
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
