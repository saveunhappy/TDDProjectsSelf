package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class Context {
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> componentClass, Type component) {
        providers.put(componentClass, () -> component);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> componentClass, Class<Implementation> implementation) {
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);

        providers.put(componentClass, new ConstructorInjectionProvider<Implementation>(injectConstructor));
    }

    private <Type> Type getType(Constructor<Type> injectConstructor) {
        try {
            Object[] array = Arrays.stream(injectConstructor.getParameters())
                    .map(it -> Context.this.get(it.getType()).orElseThrow(DependencyNotFoundException::new)).toArray();
            return injectConstructor.newInstance(array);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    class ConstructorInjectionProvider<T> implements Provider<T> {
        private Constructor<T> injectConstructor;

        public ConstructorInjectionProvider(Constructor<T> injectConstructor) {
            this.injectConstructor = injectConstructor;
        }

        @Override
        public T get() {
            return getType(injectConstructor);
        }
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if (injectConstructors.size() > 1) throw new IllegalComponentException();

        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

    public <Type> Optional<Type> get(Class<Type> type) {
        return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get());
    }
}
