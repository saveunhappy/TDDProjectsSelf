package com.geektime.tdd;

import java.util.List;

interface ComponentProvider<T> {
    T get(Context context);

    default List<Ref> getDependencies(){
        return List.of();
    }


}
