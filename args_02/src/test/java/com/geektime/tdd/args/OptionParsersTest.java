package com.geektime.tdd.args;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.IntFunction;

import static com.geektime.tdd.args.OptionParsersTest.BooleanOptionParserTest.option;
import static java.util.Arrays.*;
import static org.junit.jupiter.api.Assertions.*;

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
            //TODO -g "this" "is" {"this","is"}
            @Test
            public void should_parse_list_value() {
                String[] value = OptionParsers.list(new IntFunction<String[]>() {
                            @Override
                            public String[] apply(int value1) {
                                return new String[value1];
                            }
                        }, String::valueOf)
                        .parse(asList("-g", "this", "is"), option("g"));
                assertArrayEquals(new String[]{"this","is"},value);
            }
            //TODO -default value []
            //TODO -d a throw exception  a不是数字，应该是数字的
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
