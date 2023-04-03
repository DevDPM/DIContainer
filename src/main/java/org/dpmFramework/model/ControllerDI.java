package org.dpmFramework.model;


public class ControllerDI {

    public static void instantiateAllAnnotations(Class<?> startClass) {
        DispatcherDI.bootstrapClassPaths(startClass);
        DispatcherDI.instantiateClassesByAnnotation();
        DispatcherDI.wireInterfaceClassToImplementedMetaClass();
    }

    public static void initializeAllAnnotations() {
        DispatcherDI.initializeAllFields();
    }

    public static void performAllControllerInit() {
        DispatcherDI.callInit();
    }
}
