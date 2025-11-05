package com.geektime.tdd;

import java.lang.reflect.Type;
import java.util.List;

interface ComponentProvider<T> {
    T get(Context context);

    default List<Ref> getDependenciesRef(){
        return getDependencies().stream().map(Ref::of).toList();
    }
    default List<Type> getDependencies() {
        return List.of();
    }

}
