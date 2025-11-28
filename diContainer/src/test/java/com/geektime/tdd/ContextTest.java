package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();
            assertSame(instance, context.get(ComponentRef.of(TestComponent.class)).get());

        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends TestComponent> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(TestComponent.class, componentType);

            Context context = config.getContext();
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor injection", ConstructorInjection.class)),
                    Arguments.of(Named.of("Field injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method injection", MethodInjection.class)));
        }

        interface Component {
            default Dependency dependency() {
                return null;
            }

        }


        static class ConstructorInjection implements TestComponent {
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

        static class FieldInjection implements TestComponent {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements TestComponent {
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
            Context context = config.getContext();
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        @Test
        public void should_retrieve_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();
            Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>() {
            }).get();
            assertSame(instance, provider.get());
        }

        @Test
        public void should_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();
            assertFalse(context.get(new ComponentRef<List<TestComponent>>() {
            }).isPresent());
        }

        @Nested
        public class WithQualifier {

            @Test
            public void should_bind_type_with_multi_qualifiers() {
                TestComponent instance = new TestComponent() {
                };
                config.bind(TestComponent.class, instance, new NamedLiteral("chosenOne"), new NamedLiteral("skyWalker"));
                Context context = config.getContext();
                TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("chosenOne"))).get();
                TestComponent skyWalker = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("skyWalker"))).get();
                assertSame(chosenOne, skyWalker);
            }

            @Test
            public void should_bind_component_with_multi_qualifiers() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ConstructorInjection.class, ConstructorInjection.class, new NamedLiteral("chosenOne"), new SkyWalkerLiteral());
                Context context = config.getContext();
                ConstructorInjection chosenOne = context.get(ComponentRef.of(ConstructorInjection.class, new NamedLiteral("chosenOne"))).get();
                ConstructorInjection skyWalker = context.get(ComponentRef.of(ConstructorInjection.class, new SkyWalkerLiteral())).get();
                assertSame(dependency, chosenOne.dependency);
                assertSame(dependency, skyWalker.dependency);


            }


            //TODO throw illegal component if illegal qualifier
            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                Component instance = new Component() {
                };
                assertThrows(IllegalComponentException.class, () -> config.bind(Component.class, instance, new TestLiteral()));
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class, () -> config.bind(ConstructorInjection.class, ConstructorInjection.class, new TestLiteral()));
            }
        }
    }


    @Nested
    public class DependencyCheck {
        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            config.bind(TestComponent.class, component);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> config.getContext());
            //这个就是哪个组件找不到哪个依赖，具体看checkDependencies这个方法
            //Field和Method找不到可以去看getDependency()这个方法，它是把构造器的参数
            //字段，还有方法的参数都添加进去了，进行concat
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(TestComponent.class, exception.getComponent());

        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("inject Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("inject Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("inject Method", MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in inject Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in inject Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in inject Method", MissingDependencyProviderMethod.class))
            );
        }

        static class MissingDependencyConstructor implements TestComponent {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {

            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {

            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingDependencyProviderField implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependency) {
            }
        }
    }


    @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
    @MethodSource
    public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                    Class<? extends Dependency> dependency) {
        //注意，这里是使用的bind，那么排列组合的时候，先说构造器，先去找Component，找到依赖Dependency
        //然后先去实例化Dependency，发现Dependency依赖Component，循环依赖了。
        //如果是构造器和字段的组合呢？构造器依赖Dependency，Dependency的字段是Component，标注了@Inject
        //在checkDependency的时候还是会循环依赖
        config.bind(TestComponent.class, component);
        config.bind(Dependency.class, dependency);

        CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                () -> config.getContext());
        List<Class<?>> classes = Arrays.asList(exception.getComponents());
        assertEquals(2, classes.size());
        assertTrue(classes.contains(TestComponent.class));
        assertTrue(classes.contains(Dependency.class));
    }


    public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
        List<Arguments> arguments = new ArrayList<>();
        List<Named<? extends Class<? extends TestComponent>>> componentInjections =
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

    static class CyclicComponentInjectConstructor implements TestComponent {
        @Inject
        public CyclicComponentInjectConstructor(Dependency dependency) {
        }
    }

    static class CyclicComponentInjectField implements TestComponent {
        @Inject
        Dependency dependency;
    }

    static class CyclicComponentInjectMethod implements TestComponent {
        @Inject
        void inject(Dependency dependency) {

        }
    }

    static class CyclicDependencyInjectConstructor implements Dependency {
        @Inject
        public CyclicDependencyInjectConstructor(TestComponent component) {
        }
    }

    static class CyclicDependencyInjectField implements Dependency {
        @Inject
        TestComponent component;
    }

    static class CyclicDependencyInjectMethod implements Dependency {
        @Inject
        void inject(TestComponent component) {

        }
    }


    @ParameterizedTest(name = "indirect cyclic dependency between {0} and {1}")
    @MethodSource
    public void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                               Class<? extends Dependency> dependency,
                                                                               Class<? extends AnotherDependency> anotherDependency) {
        config.bind(TestComponent.class, component);
        config.bind(Dependency.class, dependency);
        config.bind(AnotherDependency.class, anotherDependency);
        CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                () -> config.getContext());
        List<Class<?>> classes = Arrays.asList(exception.getComponents());
        assertEquals(3, classes.size());
        assertTrue(classes.contains(TestComponent.class));
        assertTrue(classes.contains(Dependency.class));
        assertTrue(classes.contains(AnotherDependency.class));
    }

    public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
        List<Arguments> arguments = new ArrayList<>();
        List<Named<? extends Class<? extends TestComponent>>> componentInjections =
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
        public IndirectCyclicAnotherDependencyInjectConstructor(TestComponent component) {
        }
    }

    static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
        @Inject
        TestComponent component;
    }

    static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
        @Inject
        void inject(TestComponent component) {

        }
    }

    static class CyclicDependencyProviderConstructor implements Dependency {
        String name = "222";
        Provider<TestComponent> component;

        @Inject
        public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {
            this.component = component;
        }
    }

    static class CyclicComponentProviderConstructor implements TestComponent {
        String name = "333";
        Provider<Dependency> dependency;

        @Inject
        public CyclicComponentProviderConstructor(Provider<Dependency> dependency) {
            this.dependency = dependency;
        }
    }

    @Test
    public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
        config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
        config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
        Context context = config.getContext();
        assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());

    }

    @Test
    public void should_not_throw_exception_if_cyclic_dependency_via_other_provider() {
        config.bind(TestComponent.class, CyclicComponentProviderConstructor.class);
        config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
        Context context = config.getContext();
        assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
        assertTrue(context.get(ComponentRef.of(Dependency.class)).isPresent());
    }

    @Nested
    public class WithQualifier {
        //TODO dependency missing if qualifier not match
        @Test
        public void should_throw_exception_if_qualifier_not_found() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(InjectConstructor.class, InjectConstructor.class);
            assertThrows(DependencyNotFoundException.class, () -> config.getContext());
        }

        static class InjectConstructor {
            @Inject
            public InjectConstructor(@SkyWalker Dependency dependency) {
            }
        }
        //TODO check cyclic dependencies with qualifier
    }
}

@Qualifier
@Documented
@Retention(RUNTIME)
@interface SkyWalker {

}

record SkyWalkerLiteral() implements SkyWalker {

    @Override
    public Class<? extends Annotation> annotationType() {
        return SkyWalker.class;
    }
}


record NamedLiteral(String value) implements jakarta.inject.Named {
    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof jakarta.inject.Named named) return Objects.equals(value,named.value());
        return false;
    }

}

record TestLiteral() implements Test {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}
