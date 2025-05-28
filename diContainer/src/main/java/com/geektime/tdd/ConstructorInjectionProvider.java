package com.geektime.tdd;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private List<Field> injectFields;

    public ConstructorInjectionProvider(Class<T> injectConstructor) {
        this.injectConstructor = getInjectConstructor(injectConstructor);
        this.injectFields = getInjectFields(injectConstructor);
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

    private static <T> List<Field> getInjectFields(Class<T> component) {
        return stream(component.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Inject.class)).toList();
    }

    @Override
    public T get(Context context) {
        try {
            Object[] array = Arrays.stream(injectConstructor.getParameters())
                    .map(p -> context.get(p.getType()).get())
                    .toArray();
            T instance = injectConstructor.newInstance(array);
            for (Field field : injectFields) {
                //这里直接调用.get()就可以，因为前面的getContext中得到Dependency
                //之后就会去校验，如果不存在就会抛出异常，所以这里就可以直接调用.get()
                //现在还没有加上Field的dependency，所以这里的getDependency()是没有用的
                //但是如果加上Field的dependency，就可以在这里校验了
                field.set(instance,context.get(field.getType()).get());
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependency() {
        return stream(injectConstructor.getParameters()).map(Parameter::getType).collect(Collectors.toList());
    }
}
