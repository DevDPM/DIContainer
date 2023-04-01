package org.example;

import org.example.annotation.Configurations;
import org.example.annotation.Enable;
import org.example.annotation.Inject;
import org.example.annotation.Service;
import org.example.model.BigClass;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Kickstarter {

    private static final Map<Class<?>, Set<BigClass>> injectables = new HashMap<>();
    private static final ClassLoader classLoader = Kickstarter.class.getClassLoader();

    private Kickstarter() {

    }

    public static void ignite() throws ClassNotFoundException {
        String rootPackagePath = Kickstarter.class.getPackageName();
        ignite(rootPackagePath);
    }

    private static void ignite(String packagePath) throws ClassNotFoundException {
        List<Class<?>> packagePathList = getClassTree(packagePath, new ArrayList<>());
        Set<Class<?>> interfaces = new HashSet<>();

        // instantiate all components into bigClasses as bigClass
        instantiateComponentsByAnnotation(packagePathList, interfaces);

        // check if interfaces were encountered -> solve it
        if (interfaces.size() > 0)
            wireInterfaceClassToImplementedBigClass(interfaces);

        packagePathList.forEach(baseClass -> {

            if (isNotValidClass(baseClass) || baseClass.isInterface() || !baseClass.isAnnotationPresent(Service.class)) {
                return;
            }

            BigClass baseClassInstance = getBigClassFromInjectables(baseClass);

            for (Field field : baseClass.getDeclaredFields()){
                if (field.isAnnotationPresent(Inject.class)) {
                    Set<BigClass> fieldInstances = injectables.get(field.getType());
                    if (fieldInstances == null) {
                        System.out.println("Field is not @Enable for wiring: " + field.getName());
                        return;
                    }
                    instantiateField(baseClassInstance, field, fieldInstances);
                }
            }
        });
    }



    private static void instantiateField(BigClass baseClassInstance, Field field, Set<BigClass> fieldInstances) {
        Class<?> annotatedClassName = field.getAnnotation(Inject.class).className();
        String annotatedStringName = field.getAnnotation(Inject.class).name();

        if (annotatedClassName.equals(Object.class) && annotatedStringName.equals("")) {
            BigClass fieldInstance = fieldInstances.stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Did not find anything at field: " + field.getName()));

            setField(field, baseClassInstance, fieldInstance);
        } else if (!annotatedStringName.equals("")) {
            BigClass fieldInstance = fieldInstances.stream()
                    .filter(e -> e.getAdditionalInfo().equals(annotatedStringName))
                    .findFirst().orElseThrow(() -> new RuntimeException("Did not find a match naming in field: " + field.getName()));
            setField(field, baseClassInstance, fieldInstance);

        } else {
            BigClass fieldInstance = fieldInstances.stream()
                    .filter(e -> e.getObservedClass().getName().equals(annotatedClassName.getName()))
                    .findFirst().orElseThrow(() -> new RuntimeException("Did not find a match naming in field: " + field.getName()));
            setField(field, baseClassInstance, fieldInstance);
        }
    }

    private static void wireInterfaceClassToImplementedBigClass(Set<Class<?>> interfaces) {
        List<BigClass[]> temp = new ArrayList<>();
            for (Class<?> baseIFClass : interfaces) {
                for (Map.Entry<Class<?>, Set<BigClass>> entity : injectables.entrySet()) {
                    for (BigClass bigClass : entity.getValue()) {
                        if (bigClass.getImplementations() == null)
                            continue;

                        for (Class<?> IFClass : bigClass.getImplementations()) {
                            if (IFClass.getName().equals(baseIFClass.getName())) {
                                BigClass interfaceClass = new BigClass();
                                interfaceClass.setObservedClass(baseIFClass);

                                BigClass[] holder = new BigClass[2];
                                holder[0] = interfaceClass;
                                holder[1] = bigClass;
                                temp.add(holder);
                            }
                        }
                    }
                }
            }
        temp.forEach(e -> {
            addInjectable(e[0].getObservedClass(), e[1]);
        });
    }

    private static void instantiateComponentsByAnnotation(List<Class<?>> packagePathList, Set<Class<?>> interfaces) {
        packagePathList.forEach(baseClass -> {
            if (isNotValidClass(baseClass))
                return;

            if (baseClass.isInterface()) {
                interfaces.add(baseClass);
                return;
            }

            if (baseClass.isAnnotationPresent(Configurations.class)) {
                for (Field field : baseClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Enable.class)) {
                        Class<?> fieldClass = field.getType();
                        BigClass bigClass = createBigClass(baseClass, fieldClass);
                        bigClass.setAdditionalInfo(field.getAnnotation(Enable.class).name());
                        addInjectable(field.getType(), bigClass);
                    }
                }
            } else if (baseClass.isAnnotationPresent(Service.class)) {
                BigClass bigClass = createBigClass(baseClass);
                bigClass.setAdditionalInfo(baseClass.getAnnotation(Service.class).name());
                addInjectable(baseClass, bigClass);
            }
        });
    }

    private static BigClass createBigClass(Class<?> baseClass) {
        return createBigClass(baseClass, baseClass);
    }

    private static BigClass createBigClass(Class<?> baseClass, Class<?> observedClass) {
        BigClass bigClass = new BigClass();
        bigClass.setObservedClass(observedClass);
        bigClass.setInstance(Objects.requireNonNull(createInstance(observedClass)));
        if (baseClass.getInterfaces().length > 0)
            bigClass.setImplementations(new HashSet<>(List.of(baseClass.getInterfaces())));
        return bigClass;
    }

    public static void printDIManagementList() {
        System.out.println("_____ DI Injectables list");
        AtomicInteger counter = new AtomicInteger(0);
        injectables.entrySet().forEach(a -> {
            System.out.println("__________");
            System.out.println(counter.incrementAndGet() + ". Class/Field -> "+a.getKey().getSimpleName());
            a.getValue().forEach(b -> {
                System.out.println("\tInstance:\t\t\t" + b.getInstance());
                System.out.println("\tObservedClass:\t\t" + b.getObservedClass());
                System.out.println("\t@Enable (name = ?):\t" + b.getAdditionalInfo());
                System.out.println();
            });
        });
    }

    private static void addInjectable(Class<?> type, BigClass bigClass) {
        if (injectables.get(type) != null) {
            Set<BigClass> value = injectables.get(type);
            if (value.add(bigClass)) {
                injectables.put(type, value);
            }
        } else {
            injectables.put(type, new HashSet<>(List.of(bigClass)));
        }
    }

    private static void setField(Field field, BigClass baseClass, BigClass fieldClass) {
        field.setAccessible(true);

        Object baseClassInstance = baseClass.getInstance();
        Object fieldInstance = fieldClass.getInstance();

        try {
            field.set(baseClassInstance, fieldInstance);
        } catch (IllegalAccessException e) {
            e.getMessage();
        }

    }

    private static boolean isNotValidClass(Class<?> baseClass) {
        if (baseClass.isEnum() || baseClass.isRecord() || baseClass.isAnnotation() || baseClass.isAnonymousClass()
        || baseClass.isMemberClass())
            return true;
        return false;
    }

    private static <T> T createInstance(Class<T> type)  {
        try {
            return type.getConstructor(new Class[]{}).newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            System.out.println("OBJECT COULD NOT NEWLY BE INSTANTIATED: " + type.getName());
            ex.getStackTrace();
        }
        return null;
    }

    private static List<Class<?>> getClassTree(String startPackagePath, List<Class<?>> packagePathList) {

        String path = startPackagePath.replace('.', '/');
        URL url = classLoader.getResource(path);

        if (url == null)
            return new ArrayList<>();

        File folder = new File(url.getPath());
        File[] fileArray = folder.listFiles();

        for (int i = 0; i < fileArray.length; i++) {
            String fileName = fileArray[i].getName();
            int index = fileName.indexOf(".");

            if (fileName.contains("$"))
                continue;

            if (!fileName.contains(".class")) {
                String childPath = startPackagePath + "." + fileName;
                getClassTree(childPath, packagePathList);
                continue;
            }

            String className = fileName.substring(0, index);

            String classNamePath = startPackagePath + "." + className;
            Class<?> foundClass;
            try {
                foundClass = Class.forName(classNamePath);
                packagePathList.add(foundClass);
            } catch (ClassNotFoundException ex) {
                ex.getStackTrace();
            }

        }
        return packagePathList;
    }

    private static BigClass getBigClassFromInjectables(Class<?> baseClass) {
        return getBigClassFromInjectables(baseClass, "");
    }

    private static BigClass getBigClassFromInjectables(Class<?> baseClass, String name) {
        BigClass bigClass = injectables.get(baseClass)
                .stream()
                .findFirst()
                .filter(e -> {
                    if (name.equals("")) {
                        return true;
                    }else {
                        return e.getAdditionalInfo().equals(name);
                    }
                })
                .orElseThrow(() -> new RuntimeException("not found"));

        return bigClass;
    }

    public static <T> T getInstanceOf(Class<T> classFileName) {
        return (T) getBigClassFromInjectables(classFileName).getInstance();
    }

    public static <T> T getInstanceOf(Class<T> classFileName, String name) {
        return (T) getBigClassFromInjectables(classFileName, name).getInstance();
    }
}