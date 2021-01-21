package io.github.revxrsal.mirror;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.revxrsal.mirror.MirrorInvocationHandler.*;

class MirrorFactory {

    private static final MirrorFactory instance = new MirrorFactory();
    private final Map<Class<?>, MethodHandle> constructors = new ConcurrentHashMap<>();
    private final Map<Class<?>, Mirror> staticInstances = new ConcurrentHashMap<>();

    public <S extends Mirror> S wrap(@NotNull Object o, Class<S> proxyType) {
        MirrorInvocationHandler invocationHandler = new MirrorInvocationHandler(o);
        S proxy = (S) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{proxyType}, invocationHandler);
        invocationHandler.setMirror(proxy);
        invocationHandler.setMirrorClass(proxyType.getSimpleName());
        return proxy;
    }

    public <S extends Mirror> S createForStatic(Class<S> proxyType) {
        return (S) staticInstances.computeIfAbsent(proxyType, c -> wrap(remap(proxyType, proxyType), proxyType));
    }

    public <S extends Mirror> S mirrorEnum(@NotNull Class<S> proxyType) {
        MirrorInvocationHandler invocationHandler = new MirrorInvocationHandler(proxyType);
        S proxy = (S) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{proxyType}, invocationHandler);
        invocationHandler.setMirror(proxy);
        invocationHandler.setMirrorClass(proxyType.getSimpleName());
        return proxy;
    }

    public <S extends Mirror> S construct(Class<S> proxyType, Object... args) {
        Class<?> handleType = MirrorInvocationHandler.remap(proxyType, proxyType);
        try {
            Object handle = constructors.computeIfAbsent(handleType, type -> {
                Class<?>[] types = getParameterTypes(args);
                try {
                    for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                        if (constructor.getParameterCount() != types.length) continue;
                        Class<?>[] params = constructor.getParameterTypes();
                        for (int i = 0; i < types.length; i++) {
                            Class<?> p = types[i];
                            Class<?> matching = Primitives.unwrap(params[i]);
                            if (p != Object.class && !matching.isAssignableFrom(p)) {
                                continue;
                            }
                            if (i + 1 == types.length)
                                return MethodHandles.lookup().unreflectConstructor(constructor);
                        }
                    }
                    Constructor<?> ctr = type.getDeclaredConstructor(types);
                    if (!ctr.isAccessible()) ctr.setAccessible(true);
                    return MethodHandles.lookup().unreflectConstructor(ctr);
                } catch (Throwable e) {
                    sneakyThrow(sanitizeStackTrace(e));
                    return null;
                }
            }).invokeWithArguments(mapArguments(args));
            MirrorInvocationHandler invocationHandler = new MirrorInvocationHandler(handle);
            S proxy = (S) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{proxyType}, invocationHandler);
            invocationHandler.setMirror(proxy);
            invocationHandler.setMirrorClass(proxyType.getSimpleName());
            return proxy;
        } catch (Throwable throwable) {
            sneakyThrow(sanitizeStackTrace(throwable));
            return null;
        }
    }

    public static Class<?>[] getParameterTypes(Object[] args) {
        if (args == null) return null;
        return Arrays.stream(args)
                .map(c -> c == null ? Object.class : c instanceof Mirror ? ((Mirror) c).getMirrorType() : c.getClass())
                .map(Primitives::unwrap)
                .toArray(Class[]::new);
    }

    @Contract("null -> null")
    public static Object[] mapArguments(Object[] args) {
        if (args == null) return null;
        return Arrays.stream(args).map(a -> a == null ? null : a instanceof Mirror ? ((Mirror) a).getMirrorTarget() : a).toArray();
    }

    public static MirrorFactory getInstance() {
        return instance;
    }
}
