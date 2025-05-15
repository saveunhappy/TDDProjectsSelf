package com.geektime.tdd;

import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<Class<?>, Class<?>> componentImplementations = new HashMap<>();

    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        providers.put(componentClass, () -> component);
    }

    public <ComponentType, ComponentImplementation extends ComponentType>
    void bind(Class<ComponentType> componentClass, Class<ComponentImplementation> implementation) {
        componentImplementations.put(componentClass, implementation);
        providers.put(componentClass, () -> getComponentType(implementation));
    }

    public <ComponentType> ComponentType get(Class<ComponentType> componentClass) {
        if (providers.containsKey(componentClass)) {
            return (ComponentType) providers.get(componentClass).get();
        }
        Class<?> implementation = componentImplementations.get(componentClass);
        return getComponentType(implementation);
    }

    private <ComponentType> ComponentType getComponentType(Class<?> implementation) {
        try {
            return (ComponentType) implementation.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
