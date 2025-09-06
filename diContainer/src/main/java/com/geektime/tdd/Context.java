package com.geektime.tdd;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public interface Context {
    <Type> Optional<Type> get(Class<Type> type);
    Optional<Object> get(ParameterizedType type);

    default Optional getType(Type type) {
        if (type instanceof ParameterizedType) return get((ParameterizedType) type);
        return get((Class<?>) type);
    }
}
