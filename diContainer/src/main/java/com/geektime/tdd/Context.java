package com.geektime.tdd;

import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        providers.put(componentClass, () -> component);
    }

    public <ComponentType, ComponentImplementation extends ComponentType>
    void bind(Class<ComponentType> componentClass, Class<ComponentImplementation> implementation) {
        providers.put(componentClass, () -> {
            try {
                return implementation.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <ComponentType> ComponentType get(Class<ComponentType> componentClass) {
        if (providers.containsKey(componentClass)) {
            return (ComponentType) providers.get(componentClass).get();
        }
        return (ComponentType) providers.get(componentClass).get();
    }

}
