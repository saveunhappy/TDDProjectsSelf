package com.geektime.tdd;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<ComponentType> {
    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef(component, null);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation annotation) {
        return new ComponentRef(component, annotation);
    }

    public static ComponentRef of(Type type) {
        return new ComponentRef(type, null);
    }

    private Type container;

    private Component component;
    private Class<ComponentType> componentType;

    public ComponentRef(Type type, Annotation qualifier) {
        init(type, qualifier);
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type, null);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.componentType = (Class<ComponentType>) container.getActualTypeArguments()[0];
        } else {
            this.componentType = (Class<ComponentType>) type;
        }
        this.component = new Component(componentType,qualifier);
    }

    public Class<?> getComponentType() {
        return component.type();
    }

    public Type getContainer() {
        return container;
    }


    public boolean isContainer() {
        return container != null;
    }

    public Component component() {
        return component;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRef<?> that = (ComponentRef<?>) o;
        return Objects.equals(container, that.container) && Objects.equals(component, that.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, component);
    }
}
