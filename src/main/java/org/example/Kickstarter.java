package org.example;

import org.example.model.ControllerDI;
import org.example.model.MetaClassRepository;
import org.example.model.MetaClassService;

public class Kickstarter {

    private Kickstarter() {
    }

    public static void ignite(Class<?> startClass) {
        ControllerDI.instantiateAllAnnotations(startClass);
        ControllerDI.initializeAllAnnotations();
        ControllerDI.performAllControllerInit();
    }

    public static void printContext(){
        MetaClassService.printDIManagementList();
    }

    public static <T> T getInstanceOf(Class<T> classFileName) {
        return (T) MetaClassRepository.getMetaClassByClass(classFileName).getInstance();
    }

    public static <T> T getInstanceOf(Class<T> classFileName, String name) {
        return (T) MetaClassRepository.getMetaClassesByClass(classFileName, name).getInstance();
    }
}