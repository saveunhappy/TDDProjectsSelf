package com.geektime.tdd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

class ContainerTest {
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class DependenciesSelection {

        @Nested
        public class Qualifier {

        }
    }

    @Nested
    public class LifecycleManagement {

    }


}


interface TestComponent {
    default Dependency dependency() {
        return null;
    }
}

interface Dependency {

}

interface AnotherDependency {

}
