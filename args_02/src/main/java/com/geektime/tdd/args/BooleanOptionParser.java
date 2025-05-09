package com.geektime.tdd.args;

import java.util.List;

class BooleanOptionParser implements OptionParser<Boolean> {
    private BooleanOptionParser() {
    }

    @Override
    public Boolean parse(List<String> arguments, Option option) {
        return SingleValueOptionParser
                .values(arguments, option, 0)
                .map(it -> true).orElse(false);
    }
}
