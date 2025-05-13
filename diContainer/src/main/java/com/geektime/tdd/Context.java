package com.geektime.tdd;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<Class<?>,Object> components = new HashMap<>();
    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        components.put(componentClass,component);

    }
    public <ComponentType> ComponentType get(Class<ComponentType> componentClass) {

        return (ComponentType) components.get(componentClass);
    }
}
