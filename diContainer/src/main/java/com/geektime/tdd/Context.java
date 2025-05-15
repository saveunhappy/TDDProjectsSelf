package com.geektime.tdd;

import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<Class<?>, Object> components = new HashMap<>();
    private Map<Class<?>, Class<?>> componentImplementations = new HashMap<>();

    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        components.put(componentClass, component);
    }

    public <ComponentType, ComponentImplementation extends ComponentType>
    void bind(Class<ComponentType> componentClass, Class<ComponentImplementation> implementation) {
        componentImplementations.put(componentClass, implementation);
    }

    public <ComponentType> ComponentType get(Class<ComponentType> componentClass) {
        if (components.containsKey(componentClass)) {
            return (ComponentType) components.get(componentClass);
        }
        Class<?> implementation = componentImplementations.get(componentClass);
        try {
            return (ComponentType) implementation.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
