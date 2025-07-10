package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Nested
public class ContextTest {
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    class TypeBinding {
        @Test
        public void should_bind_type_to_a_specific_instance() throws Exception {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            assertSame(instance, context.get(Component.class).get());

        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);

            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor injection", ConstructorInjection.class)),
                    Arguments.of(Named.of("Field injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method injection", MethodInjection.class)));
        }


        static class ConstructorInjection implements Component {
            Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements Component {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements Component {
            Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        //Context
        //TODO could get Provider<T> from context

        @Test
        public void should_retrieve_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            Context context = config.getContext();
            ParameterizedType type = (ParameterizedType) new TypeLiteral<Provider<Component>>() {}.getType();
            assertEquals(Provider.class,type.getRawType());
            assertEquals(Component.class,type.getActualTypeArguments()[0]);
        }

        static abstract class TypeLiteral<T> {
            public Type getType(){
                return ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            }
        }

    }


    @Nested
    public class DependencyCheck {
        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends Component> component) {
            config.bind(Component.class, component);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> config.getContext());
            //这个就是哪个组件找不到哪个依赖，具体看checkDependencies这个方法
            //Field和Method找不到可以去看getDependency()这个方法，它是把构造器的参数
            //字段，还有方法的参数都添加进去了，进行concat
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());

        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("inject Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("inject Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("inject Method", MissingDependencyMethod.class)));
        }

        static class MissingDependencyConstructor implements Component {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {

            }
        }

        static class MissingDependencyField implements Component {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements Component {
            @Inject
            void install(Dependency dependency) {

            }
        }
    }


    @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
    @MethodSource
    public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends Component> component,
                                                                    Class<? extends Dependency> dependency) {
        //注意，这里是使用的bind，那么排列组合的时候，先说构造器，先去找Component，找到依赖Dependency
        //然后先去实例化Dependency，发现Dependency依赖Component，循环依赖了。
        //如果是构造器和字段的组合呢？构造器依赖Dependency，Dependency的字段是Component，标注了@Inject
        //在checkDependency的时候还是会循环依赖
        config.bind(Component.class, component);
        config.bind(Dependency.class, dependency);

        CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                () -> config.getContext());
        List<Class<?>> classes = Arrays.asList(exception.getComponents());
        assertEquals(2, classes.size());
        assertTrue(classes.contains(Component.class));
        assertTrue(classes.contains(Dependency.class));
    }


    public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
        List<Arguments> arguments = new ArrayList<>();
        List<Named<? extends Class<? extends Component>>> componentInjections =
                List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                        Named.of("Inject Field", CyclicComponentInjectField.class),
                        Named.of("Inject Method", CyclicComponentInjectMethod.class));
        List<Named<? extends Class<? extends Dependency>>> dependencyInjections =
                List.of(Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", CyclicDependencyInjectField.class),
                        Named.of("Inject Method", CyclicDependencyInjectMethod.class));
        for (Named component : componentInjections) {
            for (Named dependency : dependencyInjections) {
                //每次传入的参数就是一个Arguments，所以list里面这么多也是一个一个去依赖的
                arguments.add(Arguments.of(component, dependency));
            }
        }
        return arguments.stream();
    }

    static class CyclicComponentInjectConstructor implements Component {
        @Inject
        public CyclicComponentInjectConstructor(Dependency dependency) {
        }
    }

    static class CyclicComponentInjectField implements Component {
        @Inject
        Dependency dependency;
    }

    static class CyclicComponentInjectMethod implements Component {
        @Inject
        void inject(Dependency dependency) {

        }
    }

    static class CyclicDependencyInjectConstructor implements Dependency {
        @Inject
        public CyclicDependencyInjectConstructor(Component component) {
        }
    }

    static class CyclicDependencyInjectField implements Dependency {
        @Inject
        Component component;
    }

    static class CyclicDependencyInjectMethod implements Dependency {
        @Inject
        void inject(Component component) {

        }
    }



    @ParameterizedTest(name = "indirect cyclic dependency between {0} and {1}")
    @MethodSource
    public void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends Component> component,
                                                                               Class<? extends Dependency> dependency,
                                                                               Class<? extends AnotherDependency> anotherDependency) {
        config.bind(Component.class, component);
        config.bind(Dependency.class, dependency);
        config.bind(AnotherDependency.class, anotherDependency);
        CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                () -> config.getContext());
        List<Class<?>> classes = Arrays.asList(exception.getComponents());
        assertEquals(3, classes.size());
        assertTrue(classes.contains(Component.class));
        assertTrue(classes.contains(Dependency.class));
        assertTrue(classes.contains(AnotherDependency.class));
    }

    public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
        List<Arguments> arguments = new ArrayList<>();
        List<Named<? extends Class<? extends Component>>> componentInjections =
                List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                        Named.of("Inject Field", CyclicComponentInjectField.class),
                        Named.of("Inject Method", CyclicComponentInjectMethod.class));
        List<Named<? extends Class<? extends Dependency>>> dependencyInjections =
                List.of(Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", IndirectCyclicDependencyInjectField.class),
                        Named.of("Inject Method", IndirectCyclicDependencyInjectMethod.class));
        List<Named<? extends Class<? extends AnotherDependency>>> anotherDependencyInjections =
                List.of(Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                        Named.of("Inject Field", IndirectCyclicAnotherDependencyInjectField.class),
                        Named.of("Inject Method", IndirectCyclicAnotherDependencyInjectMethod.class));
        for (Named component : componentInjections) {
            for (Named dependency : dependencyInjections) {
                for (Named anotherDependency : anotherDependencyInjections) {
                    //每次传入的参数就是一个Arguments，所以list里面这么多也是一个一个去依赖的
                    arguments.add(Arguments.of(component, dependency, anotherDependency));
                }
            }
        }
        return arguments.stream();
    }

    static class IndirectCyclicDependencyInjectConstructor implements Dependency {
        @Inject
        public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
        }
    }

    static class IndirectCyclicDependencyInjectField implements Dependency {
        @Inject
        AnotherDependency anotherDependency;
    }

    static class IndirectCyclicDependencyInjectMethod implements Dependency {
        @Inject
        void inject(AnotherDependency anotherDependency) {

        }
    }

    static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
        @Inject
        public IndirectCyclicAnotherDependencyInjectConstructor(Component component) {
        }
    }

    static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
        @Inject
        Component component;
    }

    static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
        @Inject
        void inject(Component component) {

        }
    }
}
