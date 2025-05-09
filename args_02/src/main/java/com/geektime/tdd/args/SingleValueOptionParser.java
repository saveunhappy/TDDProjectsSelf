package com.geektime.tdd.args;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

class SingleValueOptionParser<T> implements OptionParser<T> {


    Function<String, T> valueParser;
    T defaultValue;

    private SingleValueOptionParser(T defaultValue, Function<String, T> valueParser) {
        this.defaultValue = defaultValue;
        this.valueParser = valueParser;
    }

    public static OptionParser<Boolean> bool() {
        return (arguments, option) -> values(arguments, option, 0)
                .map(it -> true).orElse(false);
    }

    public static <T> SingleValueOptionParser<T> createSingleValueOptionParser(T defaultValue, Function<String, T> valueParser) {
        return new SingleValueOptionParser<T>(defaultValue, valueParser);
    }

    @Override
    public T parse(List<String> arguments, Option option) {

        return getT(arguments, option);

    }

    private static <T> T getT(List<String> arguments, Option option) {
        return values(arguments, option, 1)
                .map(it -> parseValue(it.get(0), valueParser))
                .orElse(defaultValue);
    }

    static Optional<List<String>> values(List<String> arguments, Option option, int expectedSize) {

        int index = arguments.indexOf("-" + option.value());
        if (index == -1) return Optional.empty();
        List<String> values = values(arguments, index);

        if (values.size() < expectedSize) throw new InsufficientException(option.value());
        if (values.size() > expectedSize) throw new TooManyArgumentsException(option.value());
        return Optional.of(values);
    }

    private static  <T> T parseValue(String value, Function<String, T> valueParser1) {
        return valueParser1.apply(value);
    }

    static List<String> values(List<String> arguments, int index) {
        int followingFlag = IntStream.range(index + 1, arguments.size())
                .filter(it -> arguments.get(it).startsWith("-"))
                .findFirst()
                .orElse(arguments.size());
        List<String> values = arguments.subList(index + 1, followingFlag);
        return values;
    }


}
