package com.geektime.tdd;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class Ref<ComponentType> {
    public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> component) {
        return new Ref(component);
    }
    public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> component, Annotation annotation) {
        return new Ref(component);
    }
    public static Ref of(Type type) {
        return new Ref(type);
    }

    private Class<?> component;
    private Type container;

    public Ref(Class<ComponentType> component) {
        init(component);
    }

    public Ref(Type type) {
        init(type);
    }

    protected Ref() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type);
    }

    private void init(Type type) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.component = (Class<?>) container.getActualTypeArguments()[0];
        } else {
            this.component = (Class<?>) type;
        }
    }

    public Class<?> getComponent() {
        return component;
    }

    public Type getContainer() {
        return container;
    }

    public boolean isContainer() {
        return container != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ref ref = (Ref) o;
        return Objects.equals(component, ref.component) && Objects.equals(container, ref.container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component);
    }
}
