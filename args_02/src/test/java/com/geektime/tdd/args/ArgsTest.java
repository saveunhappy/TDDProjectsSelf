package com.geektime.tdd.args;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgsTest {
    // -l -p 8080 -d /usr/logs
    //[-l],[-p,8080],[-d,/usr/logs]
    //{-l:[],-p:[8080],-d:[/usr/logs]}
    //Single Option:

    @Test
    public void should_parse_int_as_option_value() {
        IntOption option = Args.parse(IntOption.class, "-p", "8080");
        assertEquals(8080, option.port());
    }


    @Test
    public void should_parse_string_as_option_value() {
        StringOption option = Args.parse(StringOption.class,"-d","/usr/logs");
        assertEquals("/usr/logs", option.directory());
    }
    record IntOption(@Option("p") int port) {

    }
    record StringOption(@Option("d") String directory) {

    }

    //sad path:
    // -bool -l t / -l t f/
    // -int  -p/ -p 8080 8081
    // -string -d/ -d /usr/logs /usr/packages
    //default value
    // -bool : false
    // -int : 0
    // -string: ""
    @Test
    public void should_example1() throws Exception {
        Options options = Args.parse(Options.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(options.logging());
        assertEquals(8080, options.port());
        assertEquals("/usr/logs", options.directory());
    }

    @Test
    @Disabled
    public void should_example2() throws Exception {
        ListOptions options = Args.parse(ListOptions.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, options.group());
        assertArrayEquals(new int[]{1, 2, -3, 5}, options.decimals());

    }

    record Options(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {

    }

    record ListOptions(@Option("g") String[] group, @Option("d") int[] decimals) {

    }
}
