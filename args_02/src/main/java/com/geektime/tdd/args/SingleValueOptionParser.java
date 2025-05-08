package com.geektime.tdd.args;

import java.util.List;
import java.util.function.Function;

class SingleValueOptionParser<T> implements OptionParser<T> {


    Function<String, T> valueParser;
    T defaultValue;

    private SingleValueOptionParser(Function<String, T> valueParser) {
        this.valueParser = valueParser;
    }

    public SingleValueOptionParser(T defaultValue, Function<String, T> valueParser) {
        this.defaultValue = defaultValue;
        this.valueParser = valueParser;
    }

    public static <T> SingleValueOptionParser<T> createSingleValueOptionParser(Function<String, T> valueParser, T defaultValue) {
        return new SingleValueOptionParser<T>(defaultValue,valueParser);
    }

    @Override
    public T parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        if (index + 1 == arguments.size() || arguments.get(index + 1).startsWith("-"))
            throw new InsufficientException(option.value());
        if ((index + 2) < arguments.size() && !arguments.get(index + 2).startsWith("-"))
            throw new TooManyArgumentsException(option.value());
        return valueParser.apply(arguments.get(index + 1));
    }

}
