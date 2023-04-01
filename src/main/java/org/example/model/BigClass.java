package org.example.model;

import java.util.Objects;
import java.util.Set;

public class BigClass {

    private Class<?> observedClass;
    private Object instance;
    private Set<Class<?>> implementations;
    private String additionalInfo = null;

    public Class<?> getObservedClass() {
        return observedClass;
    }

    public void setObservedClass(Class<?> observedClass) {
        this.observedClass = observedClass;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Set<Class<?>> getImplementations() {
        return implementations;
    }

    public void setImplementations(Set<Class<?>> implementations) {
        this.implementations = implementations;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BigClass bigClass = (BigClass) o;
        return Objects.equals(additionalInfo, bigClass.additionalInfo) && Objects.equals(observedClass, bigClass.observedClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(additionalInfo);
    }
}
