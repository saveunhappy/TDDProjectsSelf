package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Context {
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> componentClass, Type component) {
        providers.put(componentClass, () -> component);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> componentClass, Class<Implementation> implementation) {
        providers.put(componentClass, () -> {
            try {
                Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);
                Object[] array = Arrays.stream(injectConstructor.getParameters())
                        .map(it -> get(it.getType())).toArray();
                return injectConstructor.newInstance(array);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation)  {
        Stream<Constructor<?>> injectConstructor = Arrays.stream(implementation.getDeclaredConstructors())
                .filter(it -> it.isAnnotationPresent(Inject.class));

        return (Constructor<Type>) injectConstructor.findFirst().orElseGet(()->{
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <Type> Type get(Class<Type> componentClass) {

        return (Type) providers.get(componentClass).get();
    }

}
