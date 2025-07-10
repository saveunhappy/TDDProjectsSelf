package com.geektime.tdd;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.*;

class InjectionProvider<T> implements ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private List<Field> injectFields;
    private List<Method> injectMethods;

    public InjectionProvider(Class<T> injectConstructor) {
        if (Modifier.isAbstract(injectConstructor.getModifiers())) throw new IllegalComponentException();
        this.injectConstructor = getInjectConstructor(injectConstructor);
        this.injectFields = getInjectFields(injectConstructor);
        this.injectMethods = getInjectMethods(injectConstructor);
        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }

    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {

        List<Method> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(injectable(current.getDeclaredMethods())
                    //由子到父的添加，子类先添加完，就到injectMethods中了，然后到父类再找到
                    //去对比子类中是否有同名方法，并且参数个数也相同，因为存在重载，这里就是都被@Inject标注了
                    //但是只能调用一次，所以就把父类的方法过滤掉了，因为子类中已经有了
                    .filter(m -> isOverrideByInjectMethod(injectMethods, m))
                    //第一轮子类的方法没有被@Inject标注，所以是空，第二轮发现父类被标注了@Inject，
                    // 所以上一步筛选出来了被父类标注的
                    //然后再去对比，和子类的那个没有被@Inject标注的那个方法名字一样不，一样的话，
                    //那就得把父类的那个方法去掉了，否则我只new了一个子类，没有@Inject父类同名的方法
                    //只是起了个同样的名字，那么父类的那个就不应该被调用
                    .filter(m -> isOverrideByNoInjectMethod(component, m))

                    .toList());
            current = current.getSuperclass();
        }
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static boolean isOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                .noneMatch(o -> isOverride(m, o));
    }

    private static boolean isOverride(Method m, Method o) {
        return o.getName().equals(m.getName())
                && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
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
            injectFields.addAll(injectable(current.getDeclaredFields()).toList());
            current = current.getSuperclass();
        }
        return injectFields;
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(Inject.class));
    }

    @Override
    public T get(Context context) {
        try {
            Object[] dependencies = Arrays.stream(injectConstructor.getParameterTypes())
                    .map(p -> context.get(p).get())
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
        return concat(concat(stream(injectConstructor.getParameters()).map(Parameter::getType),
                        injectFields.stream().map(Field::getType)),
                //flatmap之后，m就是method本身，但是需要的是所有方法的参数，一个方法可能有两个参数，
                //两个方法就是四个参数，需要组合成一个stream，那就每个都是stream(m.getParameterTypes())
                //最后两两合并，变成一个list。
                injectMethods.stream().flatMap(m -> stream(m.getParameterTypes())))
                .collect(Collectors.toList());
    }
}
