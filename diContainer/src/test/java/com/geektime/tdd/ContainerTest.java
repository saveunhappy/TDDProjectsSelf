package com.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

class ContainerTest {
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class ComponentConstruction {

        @Test
        public void should_bind_type_to_a_specific_type() {
            Component component = new Component() {
            };
            config.bind(Component.class, component);
            assertSame(component, config.getContext().get(Component.class).get());

        }

        @Test
        public void should_return_empty_if_component_not_defined() throws Exception {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        public class DependencyCheck {

            @Test
            public void should_throw_exception_if_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () ->
                        config.getContext());
                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());

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
        }

    }

    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }


}


interface Component {

}

interface Dependency {

}
 class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

 class ComponentWithInjectionConstructor implements Component {
    Dependency dependency;

    @Inject
    public ComponentWithInjectionConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}
 class DependencyDependedOnComponent implements Dependency {
    Component component;

    @Inject
    public DependencyDependedOnComponent(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}
 class DependencyWithInjectionConstructor implements Dependency {
    String dependency;

    @Inject
    public DependencyWithInjectionConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}
 class ComponentWithMultiInjectionConstructor implements Component {
    @Inject
    public ComponentWithMultiInjectionConstructor(String name) {
    }

    @Inject
    public ComponentWithMultiInjectionConstructor(String name, Double value) {
    }
}
 class ComponentWithoutInjectionConstructorNorDefaultConstructor implements Component {
    public ComponentWithoutInjectionConstructorNorDefaultConstructor(String name) {

    }
}
 class DependencyDependedOnAnotherDependency implements Dependency {
    AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}

interface AnotherDependency {

}
 class AnotherDependencyDependedOnComponent implements AnotherDependency {
    Component component;

    @Inject

    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}
