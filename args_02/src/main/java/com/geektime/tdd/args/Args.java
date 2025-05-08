package com.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Args {

    public static <T> T parse(Class<T> optionsClass, String... args) {
        try {
            List<String> arguments = Arrays.asList(args);
            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
            Parameter[] parameters = constructor.getParameters();
            Object[] values = Arrays.stream(parameters)
                    .map(it -> parseOption(arguments, it)).
                    toArray();
            return (T) constructor.newInstance(values);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    interface OptionParser {
        Object parse(List<String> arguments, Option option);
    }

    static class BooleanOptionParser implements OptionParser {

        @Override
        public Object parse(List<String> arguments, Option option) {
            return arguments.contains("-" + option.value());
        }
    }

    static class IntOptionParser implements OptionParser {

        @Override
        public Object parse(List<String> arguments, Option option) {
            int index = arguments.indexOf("-" + option.value());
            return Integer.parseInt(arguments.get(index + 1));
        }
    }

    static class StringOptionParser implements OptionParser {

        @Override
        public Object parse(List<String> arguments, Option option) {
            int index = arguments.indexOf("-" + option.value());
            return arguments.get(index + 1);
        }
    }
    private static Map<Class<?>, OptionParser> PARSER = Map.of(
            boolean.class, new BooleanOptionParser(),
            int.class, new IntOptionParser(),
            String.class, new StringOptionParser()
    );
    private static Object parseOption(List<String> arguments, Parameter parameter) {
        Option option = parameter.getAnnotation(Option.class);
        Class<?> type = parameter.getType();
        return getOptionParser(type).parse(arguments, option);
    }


    private static OptionParser getOptionParser(Class<?> type) {
        OptionParser parser = null;
        if (type == boolean.class) {
            parser = new BooleanOptionParser();
        }
        if (type == int.class) {
            parser = new IntOptionParser();
        }
        if (type == String.class) {
            parser = new StringOptionParser();
        }
        return parser;
    }

}
