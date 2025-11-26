package com.geektime.tdd;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.Arrays.stream;

public class ContextConfig {
    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    private Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation ... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), context -> instance);
        }
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }
    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation,Annotation ... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type,qualifier), new InjectionProvider<>(implementation));
        }
    }

    record Component(Class type, Annotation qualifier) {
    }

    public Context getContext() {
        //components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        //bind过的
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {

            @Override
            public <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref) {
                if(ref.getQualifier() != null){
                    return Optional.ofNullable(components.get(new Component(ref.getComponent(), ref.getQualifier())))
                            .map(provider ->((ComponentType) provider.get(this)));

                }

                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(getComponent(ref)).map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(getComponent(ref))
                        .map(provider ->((ComponentType) provider.get(this)));
            }
        };
    }

    private <ComponentType> ComponentProvider<?> getComponent(Ref<ComponentType> ref) {
        //return components.get(new Component(ref.getComponent(),ref.getQualifier());
        return providers.get(ref.getComponent());
    }

    private void checkDependencies(/* Component */Class<?> component, Stack<Class<?>> visiting) {
//        for (Context.Ref dependency : components.get(component).getDependencies()) {
        for (Ref dependency : providers.get(component).getDependencies()) {
//            if (!components.containsKey(new Component(dependency.getComponent())))
            if (!providers.containsKey(dependency.getComponent()))
                throw new DependencyNotFoundException(component, dependency.getComponent());
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponent())) throw new CyclicDependenciesFoundException(visiting);
                visiting.push(dependency.getComponent());
//                checkDependencies(new Component(dependency.getComponent()), visiting);
                checkDependencies(dependency.getComponent(), visiting);
                visiting.pop();
            }
        }
    }


}
