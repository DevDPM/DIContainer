package org.dpmFramework;

import org.dpmFramework.model.ControllerDI;
import org.dpmFramework.model.MetaClassRepository;
import org.dpmFramework.model.MetaClassService;

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