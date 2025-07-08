package com.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Nested
public class InjectTest {
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class ConstructorInjection {
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {
            ContainerTest.Component component = getComponent(ContainerTest.Component.class, ContainerTest.ComponentWithDefaultConstructor.class);
            assertNotNull(component);
            assertTrue(component instanceof ContainerTest.ComponentWithDefaultConstructor);
        }

        private <T, R extends T> T getComponent(Class<T> type, Class<R> implementation) {
            config.bind(type, implementation);
            T instance = config.getContext().get(type).get();
            return instance;
        }

        @Test
        public void should_bind_type_to_a_class_with_injection_constructor() {
            ContainerTest.Dependency dependency = new ContainerTest.Dependency() {
            };
            config.bind(ContainerTest.Dependency.class, dependency);

            ContainerTest.Component instance = getComponent(ContainerTest.Component.class, ContainerTest.ComponentWithInjectionConstructor.class);
            assertNotNull(instance);
            assertTrue(instance instanceof ContainerTest.ComponentWithInjectionConstructor);
            assertEquals(dependency, ((ContainerTest.ComponentWithInjectionConstructor) instance).getDependency());

        }

        @Test
        public void should_bind_type_to_a_class_with_transitive_dependency() {
            config.bind(ContainerTest.Component.class, ContainerTest.ComponentWithInjectionConstructor.class);
            config.bind(ContainerTest.Dependency.class, ContainerTest.DependencyWithInjectionConstructor.class);
            config.bind(String.class, "dependency String");
            ContainerTest.Component instance = config.getContext().get(ContainerTest.Component.class).get();
            assertNotNull(instance);
            ContainerTest.Dependency dependency = ((ContainerTest.ComponentWithInjectionConstructor) instance).getDependency();
            assertNotNull(dependency);
            assertEquals("dependency String",
                    ((ContainerTest.DependencyWithInjectionConstructor) dependency).getDependency());
        }


        @Test
        public void should_throw_exception_if_multi_inject_constructor_provided() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>((Class<?>) ContainerTest.ComponentWithMultiInjectionConstructor.class));
        }

        @Test
        public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>((Class<?>) ContainerTest.ComponentWithoutInjectionConstructorNorDefaultConstructor.class);
            });
        }

        @Test
        public void should_include_dependency_from_inject_constructor() {
            ConstructorInjectionProvider<ContainerTest.ComponentWithInjectionConstructor> provider = new ConstructorInjectionProvider<>(ContainerTest.ComponentWithInjectionConstructor.class);
            assertArrayEquals(new Class<?>[]{ContainerTest.Dependency.class}, provider.getDependency().toArray(Class<?>[]::new));
        }


    }

    @Nested
    public class FieldInjection {
        static class ComponentWithFieldInjection {
            @Inject
            ContainerTest.Dependency dependency;
        }

        static class FinalInjectField {
            @Inject
            final ContainerTest.Dependency dependency = null;
        }

        static class SubclassWithFieldInjection extends FieldInjection.ComponentWithFieldInjection {
        }
        //TODO provided dependency information for field injection


        @Test
        public void should_inject_dependency_via_field() {
            ContainerTest.Dependency dependency = new ContainerTest.Dependency() {
            };
            config.bind(ContainerTest.Dependency.class, dependency);
            config.bind(FieldInjection.ComponentWithFieldInjection.class, FieldInjection.ComponentWithFieldInjection.class);

            FieldInjection.ComponentWithFieldInjection component = config.getContext().get(FieldInjection.ComponentWithFieldInjection.class).get();
            assertSame(dependency, component.dependency);

        }

        @Test
        public void should_inject_dependency_via_superclass_inject_field() throws Exception {
            ContainerTest.Dependency dependency = new ContainerTest.Dependency() {
            };
            config.bind(ContainerTest.Dependency.class, dependency);
            config.bind(FieldInjection.SubclassWithFieldInjection.class, FieldInjection.SubclassWithFieldInjection.class);
            FieldInjection.SubclassWithFieldInjection component = config.getContext().get(FieldInjection.SubclassWithFieldInjection.class).get();
            assertSame(dependency, component.dependency);
        }

        @Test
        public void should_throw_exception_if_inject_field_is_final() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FieldInjection.FinalInjectField.class));
        }

        @Test
        public void should_include_field_dependency_in_dependencies() {
            //类的测试，
            ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
            //注意看getDependency()的实现，就是根据Constructor的参数是什么类型就添加到这个List中去
            assertArrayEquals(new Class<?>[]{ContainerTest.Dependency.class}, provider.getDependency().toArray());
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
            config.bind(MethodInjection.InjectMethodWithNoDependency.class, MethodInjection.InjectMethodWithNoDependency.class);
            MethodInjection.InjectMethodWithNoDependency component = config.getContext().get(MethodInjection.InjectMethodWithNoDependency.class).get();
            assertTrue(component.called);

        }

        static class SuperClassWithInjectMethod {
            int superCalled = 0;

            @Inject
            void install() {
                superCalled++;
            }
        }

        static class SubClassWithInjectMethod extends MethodInjection.SuperClassWithInjectMethod {
            int subCalled = 0;

            @Inject
            void installAnother() {
                subCalled = superCalled + 1;
            }
        }

        @Test
        public void should_inject_dependencies_via_inject_method_from_superclass() {
            config.bind(MethodInjection.SubClassWithInjectMethod.class, MethodInjection.SubClassWithInjectMethod.class);
            MethodInjection.SubClassWithInjectMethod component = config.getContext().get(MethodInjection.SubClassWithInjectMethod.class).get();
            //如果是先是子后是父，那么刚开始，superCalled是0，superCalled + 1是1，然后再调用父，父是0，加1还是1，就该都是1
            //如果先是父后是子，那么父先加了，是1，然后子的superCalled是1,1 + 1就是2
            assertEquals(1, component.superCalled);
            assertEquals(2, component.subCalled);
        }

        static class SubClassOverrideSuperClassWithInject extends MethodInjection.SuperClassWithInjectMethod {
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_once_if_subclass_override_inject_method_with_inject() throws Exception {
            config.bind(MethodInjection.SubClassOverrideSuperClassWithInject.class, MethodInjection.SubClassOverrideSuperClassWithInject.class);
            MethodInjection.SubClassOverrideSuperClassWithInject component = config.getContext().get(MethodInjection.SubClassOverrideSuperClassWithInject.class).get();
            assertEquals(1, component.superCalled);
        }

        static class SubClassOverrideSuperClassWithNoInject extends MethodInjection.SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_not_call_inject_method_if_override_with_no_inject() throws Exception {
            config.bind(MethodInjection.SubClassOverrideSuperClassWithNoInject.class, MethodInjection.SubClassOverrideSuperClassWithNoInject.class);
            MethodInjection.SubClassOverrideSuperClassWithNoInject component = config.getContext().get(MethodInjection.SubClassOverrideSuperClassWithNoInject.class).get();
            assertEquals(0, component.superCalled);
        }

        static class InjectMethodWithDependency {
            ContainerTest.Dependency dependency;

            @Inject
            void install(ContainerTest.Dependency dependency) {
                this.dependency = dependency;
            }
        }

        @Test
        public void should_inject_dependency_via_inject_method() {
            ContainerTest.Dependency dependency = new ContainerTest.Dependency() {
            };
            config.bind(ContainerTest.Dependency.class, dependency);
            config.bind(MethodInjection.InjectMethodWithDependency.class, MethodInjection.InjectMethodWithDependency.class);
            MethodInjection.InjectMethodWithDependency injectMethodWithDependency = config.getContext().get(MethodInjection.InjectMethodWithDependency.class).get();
            assertSame(injectMethodWithDependency.dependency, dependency);
        }

        @Test
        public void should_include_dependencies_from_inject_method() {
            ConstructorInjectionProvider<MethodInjection.InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithDependency.class);
            assertArrayEquals(new Class<?>[]{ContainerTest.Dependency.class}, provider.getDependency().toArray());
        }

        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install() {

            }
        }

        @Test
        public void should_throw_exception_if_inject_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithTypeParameter.class));
        }

    }


}
