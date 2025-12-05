package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.*;

class InjectionProvider<T> implements ComponentProvider<T> {
    private List<Field> injectFields;
    private List<Method> injectMethods;

    private List<ComponentRef> dependencies;

    private Injectable<Constructor<T>> injectConstructor;

    private List<Injectable<Method>> injectableMethods;

    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw new IllegalComponentException();
        this.injectConstructor = Injectable.of(getInjectConstructor(component));
        this.injectableMethods = getInjectMethods(component).stream().map(Injectable::of).collect(Collectors.toList());

        this.injectFields = getInjectFields(component);
        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectableMethods.stream().map(Injectable::element).anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
        dependencies = getDependencies();

    }

    record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] require) {
        private static <Element extends Executable> Injectable<Element> of(Element constructor) {
            return new Injectable<>(constructor, stream(constructor.getParameters()).map(InjectionProvider::toComponentRef).toArray(ComponentRef<?>[]::new));
        }

        Object[] toDependencies(Context context) {
            return stream(require())
                    .map(ref -> context.get(ref).get())
                    .toArray();
        }
    }

    @Override
    public List<ComponentRef> getDependencies() {
        return concat(concat(stream(injectConstructor.require()),
                        injectFields.stream().map(InjectionProvider::toComponentRef)),
                injectableMethods.stream().flatMap(m -> stream(m.element().getParameters()).map(InjectionProvider::toComponentRef)))
                .toList();

    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();

        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(implementation));
    }

    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {

        List<Method> injectMethods = traverse(component,
                (methods, current) -> injectable(current.getDeclaredMethods())
                        //由子到父的添加，子类先添加完，就到injectMethods中了，然后到父类再找到
                        //去对比子类中是否有同名方法，并且参数个数也相同，因为存在重载，这里就是都被@Inject标注了
                        //但是只能调用一次，所以就把父类的方法过滤掉了，因为子类中已经有了
                        .filter(m -> isOverrideByInjectMethod(methods, m))
                        //第一轮子类的方法没有被@Inject标注，所以是空，第二轮发现父类被标注了@Inject，
                        // 所以上一步筛选出来了被父类标注的
                        //然后再去对比，和子类的那个没有被@Inject标注的那个方法名字一样不，一样的话，
                        //那就得把父类的那个方法去掉了，否则我只new了一个子类，没有@Inject父类同名的方法
                        //只是起了个同样的名字，那么父类的那个就不应该被调用
                        .filter(m -> isOverrideByNoInjectMethod(component, m))
                        .toList());
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        return traverse(component, (injectFields1, current) -> injectable(current.getDeclaredFields()).toList());
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> members = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            //注意，这里是current
            members.addAll(finder.apply(members, current));
            current = current.getSuperclass();
        }
        return members;
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(Inject.class));
    }

    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructor.element().newInstance(injectConstructor.toDependencies(context));
            for (Field field : injectFields) {
                //这里直接调用.get()就可以，因为前面的getContext中得到Dependency
                //之后就会去校验，如果不存在就会抛出异常，所以这里就可以直接调用.get()
                //现在还没有加上Field的dependency，所以这里的getDependency()是没有用的
                //但是如果加上Field的dependency，就可以在这里校验了
                field.set(instance, toDependencies(context, field));
            }
            for (Injectable<Method> injectableMethod : injectableMethods) {
                injectableMethod.element().invoke(instance, toDependencies(context, injectableMethod.element()));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object toDependencies(Context context, Field field) {
        return toDependency(context, toComponentRef(field));
    }

    private Object[] toDependencies(Context context, Executable executable) {
        return Arrays.stream(executable.getParameters())
                .map(p -> toDependency(context, toComponentRef(p)))
                .toArray();
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


    private static Annotation getQualifier(AnnotatedElement field) {
        List<Annotation> qualifiers = stream(field.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).collect(Collectors.toList());
        if (qualifiers.size() > 1) throw new IllegalComponentException();
        return qualifiers.stream().findFirst().orElse(null);
    }

    private static Object toDependency(Context context, ComponentRef of) {
        return context.get(of).get();
    }

    private static ComponentRef toComponentRef(Parameter p) {
        return ComponentRef.of(p.getParameterizedType(), getQualifier(p));
    }


    private static ComponentRef toComponentRef(Field field) {
        return ComponentRef.of(field.getGenericType(), getQualifier(field));
    }
}
