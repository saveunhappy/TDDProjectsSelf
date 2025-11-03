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
                if (isContainer(type)) return getContainer((ParameterizedType) type);
                return getComponent((Class<?>) type);
            }

            private Optional getComponent(Class type) {
                // 所以这里的 this 就是指代当前创建的 Context 匿名实现类的实例本身
                return Optional.ofNullable(providers.get(type)).map(provider -> provider.get(this));
            }

            private Optional<Object> getContainer(ParameterizedType type) {
                if (type.getRawType() != Provider.class) return Optional.empty();
                return Optional.ofNullable(providers.get(getComponentType(type))).map(provider -> (Provider<Object>) () -> provider.get(this));
            }
        };
    }

    private Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    private boolean isContainer(Type type) {
        return type instanceof ParameterizedType;
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            if (isContainer(dependency)) checkContainerTypeDependency(component, dependency);
            else checkComponentDependency(component, visiting, (Class<?>) dependency);
        }
    }

    private void checkContainerTypeDependency(Class<?> component, Type dependency) {
        if (!providers.containsKey(getComponentType(dependency)))
            throw new DependencyNotFoundException(component, getComponentType(dependency));
    }

    private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
        if (visiting.contains(dependency)) throw new CyclicDependenciesFoundException(visiting);
        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }


}
