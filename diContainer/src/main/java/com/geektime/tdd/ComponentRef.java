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

    private Annotation qualifier;

    public ComponentRef(Type type, Annotation qualifier) {
        init(type);
        this.qualifier = qualifier;
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type);
    }

    private void init(Type type) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.componentType = (Class<ComponentType>) container.getActualTypeArguments()[0];
        } else {
            this.componentType = (Class<ComponentType>) type;
        }
    }

    public Class<?> getComponentType() {
        return componentType;
    }

    public Type getContainer() {
        return  container;
    }


    public boolean isContainer() {
        return container != null;
    }

    public Annotation getQualifier() {
        return qualifier;
    }

    public Component component(){
        return component;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRef<?> ref = (ComponentRef<?>) o;
        return Objects.equals(componentType, ref.componentType) && Objects.equals(container, ref.container) && Objects.equals(qualifier, ref.qualifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentType, container, qualifier);
    }
}
