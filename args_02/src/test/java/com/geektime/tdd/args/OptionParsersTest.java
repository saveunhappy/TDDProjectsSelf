package com.geektime.tdd.args;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.IntFunction;

import static com.geektime.tdd.args.OptionParsersTest.BooleanOptionParserTest.option;
import static java.util.Arrays.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OptionParsersTest {
    @Nested
    class UnaryOptionParser {

        @Test
        public void should_not_accept_extra_argument_for_single_valued_option() {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () ->
                    OptionParsers.unary(0, Integer::parseInt)
                            .parse(asList("-p", "8080", "8081"), option("p")));
            assertEquals("p", e.getOption());
        }
        @Test
        public void should_throw_exception_when_value_parser_error() {
        //自己写的，不够通用，但是举了具体的例子
            IllegalValueException e = assertThrows(IllegalValueException.class, () ->
                    OptionParsers.unary(0, Integer::parseInt)
                            .parse(asList("-p", "Not Integer"), option("p")));
            assertEquals("p", e.getOption());
        }

        @ParameterizedTest
        @ValueSource(strings = {"-p -l", "-p"})
        public void should_not_accept_insufficient_argument_for_single_valued_option(String arguments) throws Exception {
            InsufficientException e = assertThrows(InsufficientException.class, () ->
                    OptionParsers.unary(0, Integer::parseInt)
                            .parse(asList(arguments.split(" ")), option("p")));
            assertEquals("p", e.getOption());
        }

        @Test
        public void should_set_default_value_to_0_for_int_option() {
            Function<String, Object> whatever = (it) -> null;
            Object defaultValue = new Object();
            assertSame(defaultValue, OptionParsers.unary(defaultValue, whatever).parse(asList(), option("p")));
        }

        @Test
        public void should_not_accept_extra_argument_for_String_single_valued_option() throws Exception {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () ->
                    OptionParsers.unary("", String::valueOf)
                            .parse(asList("-d", "/usr/logs", "/usr/vars"), option("d")));
            assertEquals("d", e.getOption());
        }

        @Test//Happy path
        public void should_parse_value_if_flag_present() {
            Object parsed = new Object();
            Function<String, Object> parse = (it) -> parsed;
            Object whatever = new Object();
            assertSame(parsed, OptionParsers.unary(whatever, parse).parse(asList("-p", "8080"), option("p")));
        }
        @Test//Happy path
        public void should_parse_value_if_flag_present_behave() throws Exception {
            Function parser = mock(Function.class);
            OptionParsers.unary(any(), parser).parse(asList("-p", "8080"), option("p"));
            verify(parser).apply("8080");
        }
    }

    @Nested
    class BooleanOptionParserTest {
        @Test
        public void should_not_accept_extra_argument_for_boolean_option() {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class,
                    () -> OptionParsers.bool().parse(Arrays.asList("-l", "t"), option("l")));
            assertEquals("l", e.getOption());
        }

        @Test
        public void should_set_default_value_to_false_if_option_not_present() {
            assertFalse(OptionParsers.bool().parse(asList(), option("l")));
        }

        @Test
        public void should_set_value_to_true_if_option_present() {
            assertTrue(OptionParsers.bool().parse(asList("-l"), option("l")));
        }

        @Nested
        class ListOptionParser {
            @Test
            public void should_parse_list_value() {
                String[] value = OptionParsers.list(String[]::new, String::valueOf)
                        .parse(asList("-g", "this", "is"), option("g"));
                assertArrayEquals(new String[]{"this", "is"}, value);
            }
            @Test
            public void should_parse_list_value_behave() {
                Function parser = mock(Function.class);
                OptionParsers.list(String[]::new, parser)
                        .parse(asList("-g", "this", "is"),
                                option("g"));
                //使用InOrder排序
                InOrder inOrder = inOrder(parser);
                inOrder.verify(parser).apply("this");
                inOrder.verify(parser).apply("is");
            }
            @Test
            public void should_not_treat_negative_int_as_flag() throws Exception {
                assertArrayEquals(new Integer[]{-1,-2},OptionParsers.list(Integer[]::new, Integer::valueOf)
                        .parse(asList("-g", "-1", "-2"), option("g")));


            }

            @Test
            public void should_use_empty_array_as_default_value() throws Exception {
                //没有-g，那么index就是-1，那么就返回null，那么就进入orElse,就是数组的长度是0
                //return Optional.ofNullable(index == -1 ? null : values(arguments, index));
                String[] value = OptionParsers.list(String[]::new, String::valueOf)
                        .parse(asList(), option("g"));
                assertEquals(0, value.length);

            }

            @Test
            public void should_throw_exception_if_value_parser_cant_parse_value() {
                Function<String, String> parser = (it) -> {
                    throw new RuntimeException();
                };
                //这个是直接把parser抛出异常，就没有去解析，就是看是否能抛出异常
                IllegalValueException e = assertThrows(IllegalValueException.class,
                        () -> OptionParsers.list(String[]::new, parser)
                        .parse(asList("-g", "this", "is"), option("g")));
                assertEquals("g", e.getOption());
                assertEquals("this", e.getValue());
            }
        }

        static Option option(String value) {
            return new Option() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return Option.class;
                }

                @Override
                public String value() {
                    return value;
                }
            };
        }
    }
}
