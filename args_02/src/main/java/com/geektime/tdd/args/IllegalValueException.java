package com.geektime.tdd.args;

public class IllegalValueException extends RuntimeException {
    String option;
    String value;

    public IllegalValueException(String option, String value) {
        this.option = option;
        this.value = value;
    }

    public String getOption() {
        return option;
    }

    public String getValue() {
        return value;
    }
}
