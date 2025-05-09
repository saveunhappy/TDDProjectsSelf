package com.geektime.tdd.args;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Function;

import static com.geektime.tdd.args.BooleanOptionParserTest.option;
import static java.util.Arrays.*;
import static org.junit.jupiter.api.Assertions.*;

public class SingleValuedOptionParserTest {
    @Test
    public void should_not_accept_extra_argument_for_single_valued_option() {
        TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () ->
                new SingleValueOptionParser<Integer>(0, Integer::parseInt)
                        .parse(asList("-p", "8080", "8081"), option("p")));
        assertEquals("p", e.getOption());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-p -l", "-p"})
    public void should_not_accept_insufficient_argument_for_single_valued_option(String arguments) throws Exception {
        InsufficientException e = assertThrows(InsufficientException.class, () ->
                new SingleValueOptionParser<Integer>(0, Integer::parseInt)
                        .parse(asList(arguments.split(" ")), option("p")));
        assertEquals("p", e.getOption());
    }

    @Test
    public void should_set_default_value_to_0_for_int_option() {
        Function<String, Object> whatever = (it) -> null;
        Object defaultValue = new Object();
        assertSame(defaultValue, new SingleValueOptionParser<>(defaultValue, whatever).parse(asList(), option("p")));
    }

    @Test
    public void should_not_accept_extra_argument_for_String_single_valued_option() throws Exception {
        TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () ->
                new SingleValueOptionParser<>("", String::valueOf)
                        .parse(asList("-d", "/usr/logs", "/usr/vars"), option("d")));
        assertEquals("d", e.getOption());
    }

    @Test//Happy path
    public void should_parse_value_if_flag_present() {
        Object parsed = new Object();
        Function<String, Object> parse = (it) -> parsed;
        Object whatever = new Object();
        assertSame(parsed,new SingleValueOptionParser<>(whatever,parse).parse(asList("-p","8080"),option("p")));
    }


}
