package com.geektime.tdd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerTest {
    Context context;

    @BeforeEach
    public void setup(){
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
            //TODO: A->B->C
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

    static class ComponentWithDefaultConstructor implements Component {
        public ComponentWithDefaultConstructor() {
        }
    }
}

