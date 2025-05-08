package com.geektime.tdd.args;

class StringOptionParser extends IntOptionParser {

    private StringOptionParser() {
        super(String::valueOf);
    }

    public static IntOptionParser createStringOptionParser() {
        return new IntOptionParser(String::valueOf);
    }
}
