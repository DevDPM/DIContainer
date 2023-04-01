package org.example.model;


public class Controller {

    public static void instantiateAllAnnotations() {
        DispatcherDI.instantiateClassesByAnnotation();
        DispatcherDI.wireInterfaceClassToImplementedMetaClass();
    }

    public static void initializeAllAnnotations() {
        DispatcherDI.initializeClasses();
    }
}
