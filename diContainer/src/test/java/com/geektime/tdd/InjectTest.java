package com.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectTest {
    Dependency dependency = mock(Dependency.class);
    Context context = mock(Context.class);

    @BeforeEach
    public void setup() {
        when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }

    @Nested
    public class ConstructorInjection {
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {
            ComponentWithDefaultConstructor instance = new ConstructorInjectionProvider<>(ComponentWithDefaultConstructor.class).get(context);
            assertNotNull(instance);
        }


        @Test
        public void should_bind_type_to_a_class_with_injection_constructor() {


            ComponentWithInjectionConstructor instance = new ConstructorInjectionProvider<>(ComponentWithInjectionConstructor.class).get(context);
            assertNotNull(instance);
            assertEquals(dependency, instance.getDependency());

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


        @Test
        public void should_inject_dependency_via_field() {

            ComponentWithFieldInjection component = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class).get(context);
            assertSame(dependency, component.dependency);

        }

        @Test
        public void should_inject_dependency_via_superclass_inject_field() throws Exception {

            SubclassWithFieldInjection component = new ConstructorInjectionProvider<>(SubclassWithFieldInjection.class).get(context);
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
            InjectMethodWithNoDependency component = new ConstructorInjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
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

            SubClassWithInjectMethod component = new ConstructorInjectionProvider<>(SubClassWithInjectMethod.class).get(context);
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

            SubClassOverrideSuperClassWithInject component = new ConstructorInjectionProvider<>(SubClassOverrideSuperClassWithInject.class).get(context);
            assertEquals(1, component.superCalled);
        }

        static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_not_call_inject_method_if_override_with_no_inject() throws Exception {

            SubClassOverrideSuperClassWithNoInject component = new ConstructorInjectionProvider<>(SubClassOverrideSuperClassWithNoInject.class).get(context);
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
            InjectMethodWithDependency injectMethodWithDependency = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class).get(context);
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
