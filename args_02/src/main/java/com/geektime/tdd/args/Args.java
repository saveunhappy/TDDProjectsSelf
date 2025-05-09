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
        } catch (IllegalOptionException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<Class<?>, OptionParser> PARSER = Map.of(
            boolean.class, SingleValueOptionParser.bool(),
            int.class, SingleValueOptionParser.createSingleValueOptionParser(0, Integer::parseInt),
            String.class, SingleValueOptionParser.createSingleValueOptionParser("", String::valueOf)
    );

    private static Object parseOption(List<String> arguments, Parameter parameter) {
        if (!parameter.isAnnotationPresent(Option.class)) throw new IllegalOptionException(parameter.getName());
        Class<?> type = parameter.getType();
        return PARSER.get(type).parse(arguments, parameter.getAnnotation(Option.class));
    }


}
