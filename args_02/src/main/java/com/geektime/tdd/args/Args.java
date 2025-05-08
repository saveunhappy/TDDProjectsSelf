package com.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;

public class Args {

    public static <T> T parse(Class<T> optionsClass, String ... args) {
        Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
        try {
            return (T) constructor.newInstance(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
