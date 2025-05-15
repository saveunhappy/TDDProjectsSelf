package com.geektime.tdd;

import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> componentClass, Type component) {
        providers.put(componentClass, () -> component);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> componentClass, Class<Implementation> implementation) {
        providers.put(componentClass, () -> {
            try {
                return implementation.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <Type> Type get(Class<Type> componentClass) {
        return (Type) providers.get(componentClass).get();
    }

}
