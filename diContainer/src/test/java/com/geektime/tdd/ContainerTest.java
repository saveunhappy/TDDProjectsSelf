package com.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerTest {
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class ComponentConstruction {

        //TODO: instance
        @Test
        public void should_bind_type_to_a_specific_type() {
            Component component = new Component() {
            };
            config.bind(Component.class, component);
            assertSame(component, config.getContext().get(Component.class).get());

        }

        //TODO: abstract class
        //TODO: interface
        @Nested
        public class ConstructorInjection {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                config.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component component = config.getContext().get(Component.class).get();
                assertNotNull(component);
                assertTrue(component instanceof ComponentWithDefaultConstructor);
            }

            @Test
            public void should_bind_type_to_a_class_with_injection_constructor() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, dependency);
                Component component = config.getContext().get(Component.class).get();
                assertNotNull(component);
                assertTrue(component instanceof ComponentWithInjectionConstructor);
                assertEquals(dependency, ((ComponentWithInjectionConstructor) component).getDependency());

            }

            @Test
            public void should_bind_type_to_a_class_with_transitive_dependency() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                config.bind(String.class, "dependency String");
                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectionConstructor) instance).getDependency();
                assertNotNull(dependency);
                assertEquals("dependency String",
                        ((DependencyWithInjectionConstructor) dependency).getDependency());
            }


            @Test
            public void should_throw_exception_if_multi_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> config.bind(Component.class, ComponentWithMultiInjectionConstructor.class));
            }

            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithoutInjectionConstructorNorDefaultConstructor.class);
                });
            }

            @Test
            public void should_throw_exception_if_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () ->
                        config.getContext());
                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());

            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() throws Exception {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () ->
                        config.getContext());
                assertEquals(String.class, exception.getDependency());
            }

            @Test
            public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);
                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
                List<Class<?>> classes = Arrays.asList(exception.getComponents());
                assertEquals(3, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
                assertTrue(classes.contains(AnotherDependency.class));
            }

            @Test
            public void should_throw_exception_if_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());
                List<Class<?>> classes = Arrays.asList(exception.getComponents());
                assertEquals(2, classes.size());
                assertTrue(classes.contains(Component.class));
                assertTrue(classes.contains(Dependency.class));
            }
        }

        @Nested
        public class FieldInjection {
            class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            //TODO inject field
            @Test
            public void should_inject_dependency_via_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

                ComponentWithFieldInjection component = config.getContext().get(ComponentWithFieldInjection.class).get();
                assertSame(dependency, component.dependency);

            }
            @Test
            public void should_create_component_with_injection_field() {
                Context context = mock(Context.class);
                Dependency dependency = mock(Dependency.class);
                when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
                //这样只会就会找到@Inject标注的方法，目前是只有方法，
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                //get的时候应该能获取到对应的依赖，因为里面有Dependency的方法，如果依赖没有那么也是就报错了，所以这里是最终要实现的
                //根据字段注入
                ComponentWithFieldInjection component = provider.get(context);
                Assert.assertSame(dependency, component.dependency);
            }
            //TODO throw exception if dependency not found
            //TODO throw exception if field is final
            //TODO throw exception if cyclic dependency

        }

        @Nested
        public class MethodInjection {

        }

    }

    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }

    interface Component {

    }

    interface Dependency {

    }

    static class ComponentWithDefaultConstructor implements Component {
        public ComponentWithDefaultConstructor() {
        }
    }


    static class ComponentWithInjectionConstructor implements Component {
        Dependency dependency;

        @Inject
        public ComponentWithInjectionConstructor(Dependency dependency) {
            this.dependency = dependency;
        }

        public Dependency getDependency() {
            return dependency;
        }
    }

    static class DependencyDependedOnComponent implements Dependency {
        Component component;

        @Inject
        public DependencyDependedOnComponent(Component component) {
            this.component = component;
        }

        public Component getComponent() {
            return component;
        }
    }

    static class DependencyWithInjectionConstructor implements Dependency {
        String dependency;

        @Inject
        public DependencyWithInjectionConstructor(String dependency) {
            this.dependency = dependency;
        }

        public String getDependency() {
            return dependency;
        }
    }

    static class ComponentWithMultiInjectionConstructor implements Component {
        @Inject
        public ComponentWithMultiInjectionConstructor(String name) {
        }

        @Inject
        public ComponentWithMultiInjectionConstructor(String name, Double value) {
        }
    }

    static class ComponentWithoutInjectionConstructorNorDefaultConstructor implements Component {
        public ComponentWithoutInjectionConstructorNorDefaultConstructor(String name) {

        }
    }

    static class DependencyDependedOnAnotherDependency implements Dependency {
        AnotherDependency anotherDependency;

        @Inject
        public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
            this.anotherDependency = anotherDependency;
        }
    }

    interface AnotherDependency {

    }

    static class AnotherDependencyDependedOnComponent implements AnotherDependency {
        Component component;

        @Inject

        public AnotherDependencyDependedOnComponent(Component component) {
            this.component = component;
        }
    }

}

