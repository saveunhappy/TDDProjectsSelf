package com.geektime.tdd.args;

import java.util.List;
import java.util.function.Function;

class SingleValueParser<T> implements OptionParser {
    Function<String, T> valueParser;

    public SingleValueParser(Function<String, T> valueParser) {
        this.valueParser = valueParser;
    }

    @Override
    public T parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        String value = arguments.get(index + 1);
        return valueParser.apply(value);
    }

}
