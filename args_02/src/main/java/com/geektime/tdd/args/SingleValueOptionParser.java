package com.geektime.tdd.args;

import java.util.List;
import java.util.function.Function;

class SingleValueOptionParser<T> implements OptionParser<T> {


    Function<String, T> valueParser;
    T defaultValue;

    public SingleValueOptionParser(T defaultValue, Function<String, T> valueParser) {
        this.defaultValue = defaultValue;
        this.valueParser = valueParser;
    }

    @Override
    public T parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        if (index == -1) return defaultValue;
        if (isReachEndOfList(arguments, index) || isFollowedByOtherFlag(arguments, index))
            throw new InsufficientException(option.value());
        if (secondArgumentIsNotAFlag(arguments, index))
            throw new TooManyArgumentsException(option.value());
        return valueParser.apply(arguments.get(index + 1));
    }

    private static boolean secondArgumentIsNotAFlag(List<String> arguments, int index) {
        return (index + 2) < arguments.size() && !arguments.get(index + 2).startsWith("-");
    }

    private static boolean isFollowedByOtherFlag(List<String> arguments, int index) {
        return arguments.get(index + 1).startsWith("-");
    }

    private static boolean isReachEndOfList(List<String> arguments, int index) {
        return index + 1 == arguments.size();
    }

}
