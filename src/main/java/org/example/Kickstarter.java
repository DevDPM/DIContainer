package org.example;

import org.example.model.Controller;
import org.example.model.MetaClassRepository;
import org.example.model.MetaClassService;

public class Kickstarter {

    private Kickstarter() {
    }

    public static void ignite(Class<?> startClass) {
        Controller.instantiateAllAnnotations(startClass);
        Controller.initializeAllAnnotations();
    }

    public static void printContext(){
        MetaClassService.printDIManagementList();
    }

    public static <T> T getInstanceOf(Class<T> classFileName) {
        return (T) MetaClassRepository.getFirstMetaClassByClass(classFileName).getInstance();
    }

    public static <T> T getInstanceOf(Class<T> classFileName, String name) {
        return (T) MetaClassRepository.getFirstMetaClassByClass(classFileName, name).getInstance();
    }
}