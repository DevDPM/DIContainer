package org.example.model;


public class Controller {

    public static void instantiateAllAnnotations(Class<?> startClass) {
        DispatcherDI.bootstrapClassPaths(startClass);
        DispatcherDI.instantiateClassesByAnnotation();
        DispatcherDI.wireInterfaceClassToImplementedMetaClass();
    }

    public static void initializeAllAnnotations() {
        DispatcherDI.initializeClasses();
    }
}
