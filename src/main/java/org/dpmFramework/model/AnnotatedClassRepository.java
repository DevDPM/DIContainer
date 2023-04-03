package org.dpmFramework.model;

import java.util.HashSet;
import java.util.Set;

public class AnnotatedClassRepository {

    private static final Set<Class<?>> configurations = new HashSet<>();
    private static final Set<Class<?>> services = new HashSet<>();
    private static final Set<Class<?>> controllers = new HashSet<>();

    public static void addControllerClass(Class<?> classFile) {
        controllers.add(classFile);
    }

    public static void addServiceClass(Class<?> classFile) {
        services.add(classFile);
    }

    public static void addConfigurationsClass(Class<?> classFile) {
        configurations.add(classFile);
    }

    public static Set<Class<?>> getControllers() {
        return controllers;
    }

    public static Set<Class<?>> getServices() {
        return services;
    }

    public static Set<Class<?>> getConfigurations() {
        return configurations;
    }

}
