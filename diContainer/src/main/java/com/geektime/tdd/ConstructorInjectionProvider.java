package com.geektime.tdd;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private List<Field> injectFields;
    private List<Method> injectMethods;

    public ConstructorInjectionProvider(Class<T> injectConstructor) {
        this.injectConstructor = getInjectConstructor(injectConstructor);
        this.injectFields = getInjectFields(injectConstructor);
        this.injectMethods = getInjectMethods(injectConstructor);
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {

        List<Method> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(stream(current.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Inject.class))
                    //由子到父的添加，子类先添加完，就到injectMethods中了，然后到父类再找到
                    //去对比子类中是否有同名方法，并且参数个数也相同，因为存在重载
                    .filter(m -> injectMethods.stream().noneMatch(o -> o.getName().equals(m.getName())
                            && Arrays.equals(o.getParameterTypes(), m.getParameterTypes())))
                    //前面筛选出来父类标注@Inject的方法，这里再过滤掉，子类同名方法，并且没有被@Inject标注的
                    .filter(m -> stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                            .noneMatch(o -> o.getName().equals(m.getName())
                                    && Arrays.equals(o.getParameterTypes(), m.getParameterTypes())))

                    .toList());
            current = current.getSuperclass();
        }
        Collections.reverse(injectMethods);
        return injectMethods;
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
        List<Field> injectFields = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            //注意，这里是current
            injectFields.addAll(stream(current.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Inject.class)).toList());
            current = current.getSuperclass();
        }
        return injectFields;
    }

    @Override
    public T get(Context context) {
        try {
            Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                    .map(p -> context.get(p.getType()).get())
                    .toArray();
            T instance = injectConstructor.newInstance(dependencies);
            for (Field field : injectFields) {
                //这里直接调用.get()就可以，因为前面的getContext中得到Dependency
                //之后就会去校验，如果不存在就会抛出异常，所以这里就可以直接调用.get()
                //现在还没有加上Field的dependency，所以这里的getDependency()是没有用的
                //但是如果加上Field的dependency，就可以在这里校验了
                field.set(instance, context.get(field.getType()).get());
            }
            for (Method method : injectMethods) {
                Object[] args = stream(method.getParameterTypes()).map(p -> context.get(p).get())
                        .toArray();
                method.invoke(instance, args);
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependency() {
        return Stream.concat(stream(injectConstructor.getParameters()).map(Parameter::getType),
                injectFields.stream().map(Field::getType)).collect(Collectors.toList());
    }
}
