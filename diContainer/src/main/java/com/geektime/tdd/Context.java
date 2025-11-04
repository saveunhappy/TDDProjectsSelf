package com.geektime.tdd;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public interface Context {
    Optional get(Ref ref);

    class Ref {
        public static Ref of(Type type) {
            //jdk17新语法，instanceof 可以直接赋值
            if (type instanceof ParameterizedType container) return new Ref(container);
            return new Ref((Class<?>) type);
        }

        private Class<?> component;
        private Type container;

        public Ref(Class<?> component) {
            this.component = component;
        }

        public Ref(ParameterizedType container) {
            this.container = container.getRawType();
            this.component = (Class<?>) container.getActualTypeArguments()[0];
        }

        public Class<?> getComponent() {
            return component;
        }

        public Type getContainer() {
            return container;
        }

        public boolean isContainer() {
            return container != null;
        }
    }
}
