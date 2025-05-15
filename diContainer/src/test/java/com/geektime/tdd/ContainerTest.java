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
            assertSame(component, context.get(Component.class));

        }

        //TODO: abstract class
        //TODO: interface
        @Nested
        public class ConstructorInjection {
            //TODO: no args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component component = context.get(Component.class);
                assertNotNull(component);
                assertTrue(component instanceof ComponentWithDefaultConstructor);
            }

            //TODO: with dependencies
            @Test
            public void should_bind_type_to_a_class_with_injection_constructor() {
                Dependency dependency = new Dependency() {
                };
                context.bind(Component.class, ComponentWithInjectionDependency.class);
                context.bind(Dependency.class, dependency);
                Component component = context.get(Component.class);
                assertNotNull(component);
                assertTrue(component instanceof ComponentWithInjectionDependency);
                assertEquals(dependency, ((ComponentWithInjectionDependency) component).getDependency());

            }

            //TODO: A->B->C
            @Test
            public void should_bind_type_to_a_class_with_transitive_dependency() {
                context.bind(Component.class, ComponentWithInjectionDependency.class);
                context.bind(Dependency.class, DependencyWithInjectionDependency.class);
                context.bind(String.class, "dependency String");
                Component instance = context.get(Component.class);
                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectionDependency) instance).getDependency();
                assertNotNull(dependency);
                assertEquals("dependency String",
                        ((DependencyWithInjectionDependency)dependency).getDependency());
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


    static class ComponentWithInjectionDependency implements Component {
        Dependency dependency;

        @Inject
        public ComponentWithInjectionDependency(Dependency dependency) {
            this.dependency = dependency;
        }

        public Dependency getDependency() {
            return dependency;
        }
    }

    static class DependencyWithInjectionDependency implements Dependency {
        String dependency;

        @Inject
        public DependencyWithInjectionDependency(String dependency) {
            this.dependency = dependency;
        }

        public String getDependency() {
            return dependency;
        }
    }

}

