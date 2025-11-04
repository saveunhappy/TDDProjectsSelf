package com.geektime.tdd;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.Arrays.stream;

public class ContextConfig {
    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, context -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        //bind过的
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {
            @Override
            public Optional get(Type type) {
                Ref ref = Ref.of(type);
                if (isContainer(type)) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return Optional.ofNullable(providers.get(ref.getComponent())).map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(providers.get(ref.getComponent())).map(provider -> provider.get(this));
            }

        };
    }

    static class Ref {
        private Class<?> component;
        private Type container;

        public Ref(Class<?> component) {
            this.component = component;
        }

        public Ref(ParameterizedType container) {
            this.container = container.getRawType();
            this.component = (Class<?>) container.getActualTypeArguments()[0];
        }

        static Ref of(Type type) {
            //jdk17新语法，instanceof 可以直接赋值
            if (type instanceof ParameterizedType container) return new Ref(container);
            return new Ref((Class<?>) type);
        }

        public Class<?> getComponent() {
            return component;
        }

        public Type getContainer() {
            return container;
        }
    }

    private Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    private boolean isContainer(Type type) {
        return type instanceof ParameterizedType;
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        Class<?> componentType = component;
        for (Type dependency : providers.get(componentType).getDependencies()) {
            if (isContainer(dependency)) checkContainerTypeDependency(componentType, dependency);
            else checkComponentDependency(componentType, visiting, (Class<?>) dependency);
        }
    }

    private void checkContainerTypeDependency(Class<?> component, Type dependency) {
        Class<?> componentType = getComponentType(dependency);
        if (!providers.containsKey(componentType))
            throw new DependencyNotFoundException(component, componentType);
    }

    private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        Class<?> componentType = dependency;
        if (!providers.containsKey(componentType)) throw new DependencyNotFoundException(component, componentType);
        if (visiting.contains(componentType)) throw new CyclicDependenciesFoundException(visiting);
        visiting.push(componentType);
        checkDependencies(componentType, visiting);
        visiting.pop();
    }


}
