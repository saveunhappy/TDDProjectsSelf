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

        providers.put(componentClass, new ConstructorInjectionProvider<>(injectConstructor));
    }
    public <Type> Optional<Type> get(Class<Type> type) {
        //最后的provider -> (Type) provider.get()其实是从provider中取出来了，然后又放到
        //这个Optional中了，所以其他的地方调用这个get方法还是要调用get方法来取出来Optional中的值
        return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get());
    }
    class ConstructorInjectionProvider<T> implements Provider<T> {
        private Constructor<T> injectConstructor;
        private boolean constructing = false;

        public ConstructorInjectionProvider(Constructor<T> injectConstructor) {
            this.injectConstructor = injectConstructor;
        }

        @Override
        public T get() {
            if (constructing) throw new CyclicDependenciesFoundException();
            try {
                constructing = true;
                Object[] array = Arrays.stream(injectConstructor.getParameters())
                        .map(it -> Context.this.get(it.getType()).orElseThrow(DependencyNotFoundException::new)).toArray();
                return injectConstructor.newInstance(array);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                constructing = false;
            }
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


}
