package com.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

class ContainerTest {
    Context context;

    @BeforeEach
    public void setup() {
        context = new Context();
    }

    @Nested
    public class ComponentConstruction {

        //TODO: instance
        @Test
        public void should_bind_type_to_a_specific_type() {
            Component component = new Component() {
            };
            context.bind(Component.class, component);
            assertSame(component, context.get(Component.class).get());

        }

        //TODO: abstract class
        //TODO: interface
        @Nested
        public class ConstructorInjection {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component component = context.get(Component.class).get();
                assertNotNull(component);
                assertTrue(component instanceof ComponentWithDefaultConstructor);
            }

            @Test
            public void should_bind_type_to_a_class_with_injection_constructor() {
                Dependency dependency = new Dependency() {
                };
                context.bind(Component.class, ComponentWithInjectionConstructor.class);
                context.bind(Dependency.class, dependency);
                Component component = context.get(Component.class).get();
                assertNotNull(component);
                assertTrue(component instanceof ComponentWithInjectionConstructor);
                assertEquals(dependency, ((ComponentWithInjectionConstructor) component).getDependency());

            }

            @Test
            public void should_bind_type_to_a_class_with_transitive_dependency() {
                context.bind(Component.class, ComponentWithInjectionConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                context.bind(String.class, "dependency String");
                Component instance = context.get(Component.class).get();
                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectionConstructor) instance).getDependency();
                assertNotNull(dependency);
                assertEquals("dependency String",
                        ((DependencyWithInjectionConstructor) dependency).getDependency());
            }


            @Test
            public void should_throw_exception_if_multi_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> context.bind(Component.class, ComponentWithMultiInjectionConstructor.class));
            }

            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithoutInjectionConstructorNorDefaultConstructor.class);
                });
            }

            @Test
            public void should_throw_exception_if_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectionConstructor.class);
                assertThrows(DependencyNotFoundException.class, () ->
                        context.get(Component.class).get());
            }

            @Test
            public void should_throw_exception_if_cyclic_dependencies_found() {
                context.bind(Component.class, ComponentWithInjectionConstructor.class);
                context.bind(Dependency.class, DependencyDependedOnComponent.class);

                assertThrows(CyclicDependenciesFoundException.class, () -> context.get(Component.class));
            }

            @Test
            public void should_throw_exception_if_transitive_cyclic_dependencies_found() throws Exception {
                context.bind(Component.class,ComponentWithInjectionConstructor.class);
                context.bind(Dependency.class,DependencyDependedOnAnotherDependency.class);
                context.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);
                assertThrows(CyclicDependenciesFoundException.class, () -> context.get(Component.class));


            }
        }

        @Nested
        public class FieldInjection {

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
    static class DependencyDependedOnAnotherDependency implements Dependency{
        AnotherDependency anotherDependency;
        @Inject
        public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
            this.anotherDependency = anotherDependency;
        }
    }

    interface AnotherDependency{

    }
    static class AnotherDependencyDependedOnComponent implements AnotherDependency{
        Component component;
        @Inject

        public AnotherDependencyDependedOnComponent(Component component) {
            this.component = component;
        }
    }

}

