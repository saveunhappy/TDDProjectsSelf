package com.geektime.tdd;

import java.util.*;

import static java.util.Arrays.stream;

public class ContextConfig {
    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, new ComponentProvider<>() {
            @Override
            public Object get(Context context) {
                return instance;
            }

            @Override
            public List<Class<?>> getDependency() {
                return List.of();
            }
        });
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new ConstructorInjectionProvider<>(implementation));
    }

    public Context getContext() {
        //bind过的
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                //在Java中，当在内部类或匿名类中使用 this 时，它指的是该内部类或匿名类的实例，而不是外部类的实例，
                // 所以这里的 this 就是指代当前创建的 Context 匿名实现类的实例本身
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependency()) {
            if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
            if (visiting.contains(dependency)) throw new CyclicDependenciesFoundException(visiting);
            visiting.push(dependency);
            checkDependencies(dependency, visiting);
            visiting.pop();
        }
    }


}
