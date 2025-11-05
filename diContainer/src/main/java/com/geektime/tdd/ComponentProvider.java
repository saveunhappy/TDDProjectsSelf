package com.geektime.tdd;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

interface ComponentProvider<T> {
    T get(Context context);

    default List<Ref> getDependenciesRef(){
        List<Ref> list = new ArrayList<>();
        for (Type type : getDependencies()) {
            Ref of = Ref.of(type);
            list.add(of);
        }
        return list;
    }
    default List<Type> getDependencies() {
        return List.of();
    }

}
