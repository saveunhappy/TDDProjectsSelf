package com.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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

        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent() {
            }
        }

        @Test
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(AbstractComponent.class));
        }

        @Test
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(Component.class));
        }

        @Nested
        public class ConstructorInjection {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                Class<Component> type = Component.class;
                Class<ComponentWithDefaultConstructor> implementation = ComponentWithDefaultConstructor.class;

                Component component = getComponent(type, implementation);
                assertNotNull(component);
                assertTrue(component instanceof ComponentWithDefaultConstructor);
            }

            private Component getComponent(Class<Component> type, Class<ComponentWithDefaultConstructor> implementation) {
                config.bind(type, implementation);
                Component component = config.getContext().get(type).get();
                return component;
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
                        () -> new ConstructorInjectionProvider<>((Class<?>) ComponentWithMultiInjectionConstructor.class));
            }

            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    new ConstructorInjectionProvider<>((Class<?>) ComponentWithoutInjectionConstructorNorDefaultConstructor.class);
                });
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                ConstructorInjectionProvider<ComponentWithInjectionConstructor> provider = new ConstructorInjectionProvider<>(ComponentWithInjectionConstructor.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependency().toArray(Class<?>[]::new));
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
            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
            }
            //TODO provided dependency information for field injection


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
            public void should_inject_dependency_via_superclass_inject_field() throws Exception {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(SubclassWithFieldInjection.class, SubclassWithFieldInjection.class);
                SubclassWithFieldInjection component = config.getContext().get(SubclassWithFieldInjection.class).get();
                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
            }

            @Test
            public void should_include_field_dependency_in_dependencies() {
                //类的测试，
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                //注意看getDependency()的实现，就是根据Constructor的参数是什么类型就添加到这个List中去
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependency().toArray());
            }


        }

        @Nested
        public class MethodInjection {
            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    called = true;
                }
            }

            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {
                config.bind(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);
                InjectMethodWithNoDependency component = config.getContext().get(InjectMethodWithNoDependency.class).get();
                assertTrue(component.called);

            }

            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                void install() {
                    superCalled++;
                }
            }

            static class SubClassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                void installAnother() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            public void should_inject_dependencies_via_inject_method_from_superclass() {
                config.bind(SubClassWithInjectMethod.class, SubClassWithInjectMethod.class);
                SubClassWithInjectMethod component = config.getContext().get(SubClassWithInjectMethod.class).get();
                //如果是先是子后是父，那么刚开始，superCalled是0，superCalled + 1是1，然后再调用父，父是0，加1还是1，就该都是1
                //如果先是父后是子，那么父先加了，是1，然后子的superCalled是1,1 + 1就是2
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }

            static class SubClassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_only_call_once_if_subclass_override_inject_method_with_inject() throws Exception {
                config.bind(SubClassOverrideSuperClassWithInject.class, SubClassOverrideSuperClassWithInject.class);
                SubClassOverrideSuperClassWithInject component = config.getContext().get(SubClassOverrideSuperClassWithInject.class).get();
                assertEquals(1, component.superCalled);
            }

            static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_inject_method_if_override_with_no_inject() throws Exception {
                config.bind(SubClassOverrideSuperClassWithNoInject.class, SubClassOverrideSuperClassWithNoInject.class);
                SubClassOverrideSuperClassWithNoInject component = config.getContext().get(SubClassOverrideSuperClassWithNoInject.class).get();
                assertEquals(0, component.superCalled);
            }

            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_method() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);
                InjectMethodWithDependency injectMethodWithDependency = config.getContext().get(InjectMethodWithDependency.class).get();
                assertSame(injectMethodWithDependency.dependency, dependency);
            }

            @Test
            public void should_include_dependencies_from_inject_method() {
                ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependency().toArray());
            }

            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {

                }
            }

            @Test
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
            }

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

