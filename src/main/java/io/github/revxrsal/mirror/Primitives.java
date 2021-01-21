package io.github.revxrsal.mirror;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class Primitives { // stolen from guava so we no longer depend on it.

    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE_TYPE;

    private Primitives() {
    }

    private static void add(Map<Class<?>, Class<?>> forward, Map<Class<?>, Class<?>> backward, Class<?> key, Class<?> value) {
        forward.put(key, value);
        backward.put(value, key);
    }

    public static <T> Class<T> unwrap(Class<T> type) {
        Objects.requireNonNull(type);
        Class<T> unwrapped = (Class<T>) WRAPPER_TO_PRIMITIVE_TYPE.get(type);
        return unwrapped == null ? type : unwrapped;
    }

    static {
        Map<Class<?>, Class<?>> primToWrap = new HashMap<>(16);
        Map<Class<?>, Class<?>> wrapToPrim = new HashMap<>(16);
        add(primToWrap, wrapToPrim, Boolean.TYPE, Boolean.class);
        add(primToWrap, wrapToPrim, Byte.TYPE, Byte.class);
        add(primToWrap, wrapToPrim, Character.TYPE, Character.class);
        add(primToWrap, wrapToPrim, Double.TYPE, Double.class);
        add(primToWrap, wrapToPrim, Float.TYPE, Float.class);
        add(primToWrap, wrapToPrim, Integer.TYPE, Integer.class);
        add(primToWrap, wrapToPrim, Long.TYPE, Long.class);
        add(primToWrap, wrapToPrim, Short.TYPE, Short.class);
        add(primToWrap, wrapToPrim, Void.TYPE, Void.class);
        WRAPPER_TO_PRIMITIVE_TYPE = Collections.unmodifiableMap(wrapToPrim);
    }
}
