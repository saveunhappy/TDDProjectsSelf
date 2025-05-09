package com.geektime.tdd.args;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

class SingleValueOptionParser<T> implements OptionParser<T> {


    Function<String, T> valueParser;
    T defaultValue;

    public SingleValueOptionParser(T defaultValue, Function<String, T> valueParser) {
        this.defaultValue = defaultValue;
        this.valueParser = valueParser;
    }

    @Override
    public T parse(List<String> arguments, Option option) {

        return values(arguments, option, 1)
                .map(it -> parseValue(it.get(0)))
                .orElse(defaultValue);

    }

    private static Optional<List<String>> values(List<String> arguments, Option option, int expectedSize) {

        Optional<List<String>> argumentList;
        int index = arguments.indexOf("-" + option.value());
        if (index == -1) argumentList = Optional.empty();
        else {
            List<String> values = values(arguments, index);

            if (values.size() < expectedSize) throw new InsufficientException(option.value());
            if (values.size() > expectedSize) throw new TooManyArgumentsException(option.value());
            argumentList = Optional.of(values);
        }
        return argumentList;
    }

    private T parseValue(String value) {
        return valueParser.apply(value);
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
