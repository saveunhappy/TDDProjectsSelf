package com.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectTest {
    Dependency dependency = mock(Dependency.class);
    Context context = mock(Context.class);

    Provider<Dependency> dependencyProvider = mock(Provider.class);

    ParameterizedType dependencyProviderType;

    @BeforeEach
    public void setup() throws NoSuchFieldException {
        //mock一下，这个就是取巧的方式，直接通过反射取这个类mock出来的Provider，如果get的是Provider<Dependency>,那么就返回一个Optional包裹着的这个对象
        dependencyProviderType = (ParameterizedType) InjectTest.class.getDeclaredField("dependencyProvider").getGenericType();
        when(context.get(eq(ComponentRef.of(Dependency.class)))).thenReturn(Optional.of(dependency));
        //为什么不直接when(context.get(eq(Provider<Dependency>.class)))？因为语法不支持，编译不通过
        when(context.get(eq(ComponentRef.of(dependencyProviderType)))).thenReturn(Optional.of(dependencyProvider));
    }

    @Nested
    public class ConstructorInjection {

        @Nested
        class Injection {
            static class DefaultConstructor {

            }

            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                DefaultConstructor instance = new InjectionProvider<>(DefaultConstructor.class).get(context);
                assertNotNull(instance);
            }


            static class InjectionConstructor {
                private Dependency dependency;

                @Inject
                public InjectionConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }

            }

            @Test
            public void should_inject_dependency_via_inject_constructor() {

                InjectionConstructor instance = new InjectionProvider<>(InjectionConstructor.class).get(context);
                assertNotNull(instance);
                assertSame(dependency, instance.dependency);
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                InjectionProvider<InjectionConstructor> provider = new InjectionProvider<>(InjectionConstructor.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());
            }

            @Test
            public void should_include_provider_type_from_inject_constructor() {
                InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray());
            }

            static class ProviderInjectConstructor {
                private Provider<Dependency> dependency;

                @Inject
                public ProviderInjectConstructor(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_provider_inject_constructor() {
                ProviderInjectConstructor instance = new InjectionProvider<>(ProviderInjectConstructor.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }


        }

        @Nested
        class IllegalInjectionConstructors {
            abstract class AbstractComponent implements TestComponent {
                @Inject
                public AbstractComponent() {

                }
            }

            class MultiInjectionConstructor implements TestComponent {
                @Inject
                public MultiInjectionConstructor(String name, Double value) {

                }

                @Inject
                public MultiInjectionConstructor(String name) {

                }
            }

            class NorDefaultConstructor implements TestComponent {
                public NorDefaultConstructor(String name) {

                }
            }

            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(AbstractComponent.class));
            }

            @Test
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(TestComponent.class));
            }

            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>((Class<? extends TestComponent>) MultiInjectionConstructor.class)
                );
            }

            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>((Class<? extends TestComponent>) NorDefaultConstructor.class));
            }

        }

        @Nested
        public class WithQualifier {
            @Test
            public void should_include_qualifier_with_dependency() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(new ComponentRef<?>[]{
                                ComponentRef.of(Dependency.class, new NamedLiteral("chosenOne"))
                        },
                        provider.getDependencies().toArray(new ComponentRef[0]));
            }

            static class InjectConstructor {
                @Inject
                public InjectConstructor(@Named("chosenOne") Dependency dependency) {
                }
            }
            //TODO throw illegal component if illegal qualifier given to injection point
        }
    }

    @Nested
    public class FieldInjection {
        @Nested
        class Injection {
            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
            }

            @Test
            public void should_inject_dependency_via_field() {
                ComponentWithFieldInjection component = new InjectionProvider<>(ComponentWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);

            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {

                SubclassWithFieldInjection component = new InjectionProvider<>(SubclassWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_dependency_from_field_dependency() {
                //类的测试，
                InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());
            }


            static class ProviderInjectField {
                @Inject
                Provider<Dependency> dependency;
            }

            @Test
            public void should_inject_provider_via_inject_constructor() {
                ProviderInjectField instance = new InjectionProvider<>(ProviderInjectField.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

            @Test
            public void should_include_provider_type_from_inject_field() {
                InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray());
            }

        }


        @Nested
        class IllegalInjectFields {
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(FinalInjectField.class));
            }
        }

        @Nested
        public class WithQualifier {
            //TODO inject with qualifier
            @Test
            public void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);
                assertArrayEquals(new ComponentRef<?>[]{ComponentRef.of(Dependency.class, new NamedLiteral("chosenOne"))},
                        provider.getDependencies().toArray(new ComponentRef[0]));
            }

            static class InjectMethod {
                @Inject
                void InjectMethod(@Named("chosenOne") Dependency dependency) {
                }
            }
            //TODO throw illegal component if illegal qualifier given to injection point
        }

    }

    @Nested
    public class MethodInjection {

        @Nested
        class Injection {

            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    this.called = true;
                }
            }

            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {

                InjectMethodWithNoDependency component = new InjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
                assertTrue(component.called);
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
                InjectMethodWithDependency component = new InjectionProvider<>(InjectMethodWithDependency.class).get(context);
                assertEquals(dependency, component.dependency);
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

                //注意，@inject标注的名字不能和父类的相同啊，否则永远调用的是子类的。
                @Inject
                void installAnother() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            public void should_inject_dependencies_via_inject_method_from_superclass() {

                SubClassWithInjectMethod component = new InjectionProvider<>(SubClassWithInjectMethod.class).get(context);
                //如果是先是子后是父，那么刚开始，superCalled是0，superCalled + 1是1，然后再调用父，父是0，加1还是1，就该都是1
                //如果先是父后是子，那么父先加了，是1，然后子的superCalled是1,1 + 1就是2
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }


            static class SubClassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
                //注意，@inject标注的名字不能和父类的相同啊，否则永远调用的是子类的。
                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_only_call_once_if_subclass_override_inject_method_with_inject() {

                SubClassOverrideSuperClassWithInject component = new InjectionProvider<>(SubClassOverrideSuperClassWithInject.class).get(context);
                assertEquals(1, component.superCalled);
            }

            static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_inject_method_if_override_with_no_inject() {

                SubClassOverrideSuperClassWithNoInject component = new InjectionProvider<>(SubClassOverrideSuperClassWithNoInject.class).get(context);
                assertEquals(0, component.superCalled);
            }

            @Test
            public void should_include_dependencies_from_inject_method() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());
            }

            static class ProviderInjectMethod {
                Provider<Dependency> dependency;

                @Inject
                void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_provider_via_inject_constructor() {
                ProviderInjectMethod instance = new InjectionProvider<>(ProviderInjectMethod.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

            @Test
            public void should_include_provider_type_from_inject_method() {
                InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray());
            }
        }

        @Nested
        class IllegalInjectMethods {
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {

                }
            }

            @Test
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectMethodWithTypeParameter.class));
            }
        }

        @Nested
        public class WithQualifier {
            //TODO inject with qualifier
            //TODO throw illegal component if illegal qualifier given to injection point
        }
    }


}
