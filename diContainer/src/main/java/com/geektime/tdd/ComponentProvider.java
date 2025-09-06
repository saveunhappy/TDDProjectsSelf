package com.geektime.tdd;

import java.lang.reflect.Type;
import java.util.List;

interface ComponentProvider<T> {
    T get(Context context);

    default List<Type> getDependencyTypes() {
        return List.of();
    }

}
