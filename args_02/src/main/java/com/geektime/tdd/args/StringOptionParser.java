package com.geektime.tdd.args;

class StringOptionParser extends IntOptionParser {

    public StringOptionParser() {
        super(String::valueOf);
    }
}
