package io.github.revxrsal.mirror;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class MirrorInvocationHandler implements InvocationHandler {

    private static final Method toString;
    private static final Method equals;
    private static final Method hashCode;
    private static final Method getMirrorTarget;
    private static final Method getMirrorType;

    static {
        try {
            getMirrorTarget = Mirror.class.getMethod("getMirrorTarget");
            getMirrorType = Mirror.class.getMethod("getMirrorType");
            toString = Object.class.getMethod("toString");
            equals = Object.class.getMethod("equals", Object.class);
            hashCode = Object.class.getMethod("hashCode");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Object handle;
    private Mirror mirror;
    private String mirrorClass;

    private final @NotNull Map<Method, MethodHandle> methods = new ConcurrentHashMap<>();
    private final @NotNull Map<Method, Mirror> mirrored = new ConcurrentHashMap<>();
    private final @NotNull Map<Method, Object> enums = new ConcurrentHashMap<>();

    public MirrorInvocationHandler(Object handle) {
        Class<?> handleType = handle instanceof Class ? (Class<?>) handle : handle.getClass();
        this.handle = handle;
        if (Mirror.class.isAssignableFrom(handleType))
            throw sanitizeStackTrace(new IllegalArgumentException("You cannot mirrorize a mirror! (Did you forget @MirrorClass?)"));
    }

    public void setMirror(Mirror mirror) {
        this.mirror = mirror;
    }

    public void setMirrorClass(String mirrorClass) {
        this.mirrorClass = mirrorClass;
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (toString.equals(method)) {
            return "{" + mirrorClass + "=" + handle + "}";
        }
        if (equals.equals(method)) {
            Object otherObject = args[0];
            if (otherObject == this) {
                return true;
            }
            if (!(otherObject instanceof Mirror))
                return false;

            Mirror other = (Mirror) otherObject;
            return Objects.equals(handle, other.getMirrorTarget());
        }
        if (hashCode.equals(method)) {
            return Objects.hashCode(mirror.getMirrorTarget());
        }
        if (getMirrorTarget.equals(method)) {
            return handle;
        }
        if (getMirrorType.equals(method)) {
            return getHandleType();
        }
        if (method.getParameterCount() == 0 && method.getDeclaringClass().isAnnotationPresent(MirrorEnum.class)) {
            return enums.computeIfAbsent(method, this::findEnum);
        }
        MethodHandle invoke = methods.computeIfAbsent(method, m -> {
            try {
                if (method.isDefault())
                    return privateLookup.newInstance(method.getDeclaringClass(), Lookup.PRIVATE)
                            .unreflectSpecial(m, m.getDeclaringClass())
                            .bindTo(proxy);
                MirrorField field = method.getAnnotation(MirrorField.class);
                if (field != null) {
                    Class<?> returnType = method.getReturnType();
                    if (Mirror.class.isAssignableFrom(returnType)) {
                        return null;
                    }
                    if (method.getParameterCount() == 1) {
                        // method is setter
                        return bind(MethodHandles.lookup().unreflectSetter(field(field.value())));
                    }
                    return bind(MethodHandles.lookup().unreflectGetter(field(field.value())));
                }
                if (Mirror.class.isAssignableFrom(method.getReturnType())) {
                    return null;
                }
                String name = getMethodName(method);
                return method(name == null ? method.getName() : name, MirrorFactory.mapArguments(args));
            } catch (Throwable t) {
                sneakyThrow(t);
                return null;
            }
        });
        if (invoke != null) {
            try {
                return invoke.invokeWithArguments(MirrorFactory.mapArguments(args));
            } catch (WrongMethodTypeException e) {
                if (handle instanceof Class)
                    throw sanitizeStackTrace(new IllegalStateException("Cannot invoke instance-method '" + method.getName() + "' from a static instance!"));
                else {
                    sneakyThrow(e);
                    return null;
                }
            } catch (Throwable t) { // the method simply threw something
                sneakyThrow(sanitizeStackTrace(t));
                return null;
            }
        } else {
            return mirrored.computeIfAbsent(method, this::mirror);
        }
    }

    static Class<?> remap(@NotNull AnnotatedElement ann, Class<?> def) {
        NmsClass nmsClass = ann.getAnnotation(NmsClass.class);
        if (nmsClass != null) {
            return GameVersion.current().getNMS(nmsClass.value());
        }
        OcbClass ocbClass = ann.getAnnotation(OcbClass.class);
        if (ocbClass != null)
            return GameVersion.current().getCraftBukkit(ocbClass.value());

        VersionedMirrorClass vmc = ann.getAnnotation(VersionedMirrorClass.class);
        if (vmc != null) {
            for (Mapping m : vmc.value()) {
                if (m.version() == GameVersion.current())
                    return vmc.type().fetch(m.name());
            }
            if (!vmc.defaultName().isEmpty())
                return vmc.type().fetch(vmc.defaultName());
        }

        MirrorClass mirrorClass = ann.getAnnotation(MirrorClass.class);
        if (mirrorClass != null) {
            try {
                return Class.forName(mirrorClass.value());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return def;
    }

    private Field field(@NotNull String name) {
        Field field = null;
        try {
            field = getHandleType().getDeclaredField(name);
            if (!field.isAccessible()) field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                field = getHandleType().getField(name);
                if (!field.isAccessible()) field.setAccessible(true);
            } catch (NoSuchFieldException noSuchFieldException) {
                for (Field f : getAllFields(getHandleType())) {
                    if (f.getName().equals(name)) {
                        if (!f.isAccessible()) f.setAccessible(true);
                        field = f;
                        break;
                    }
                }
                if (field == null) {
                    throw sanitizeStackTrace(new IllegalArgumentException("Cannot find field '" + name + "' in " + getHandleType()));
                }
            }
        }
        return field;
    }

    private MethodHandle method(@NotNull String name, Object[] args) {
        if (args == null) args = new Object[0];
        Class<?>[] types = MirrorFactory.getParameterTypes(args);
        try {
            for (Method method : getAllMethods(getHandleType())) {
                if (method.getName().equals(name)) {
                    Class<?>[] params = method.getParameterTypes();
                    if (args.length == params.length && params.length == 0) {
                        return bind(MethodHandles.lookup().unreflect(method));
                    }
                    for (int i = 0; i < types.length; i++) {
                        Class<?> p = types[i];
                        Class<?> matching = Primitives.unwrap(params[i]);
                        if (p != Object.class && !matching.isAssignableFrom(p)) {
                            continue;
                        }
                        if (i + 1 == types.length) {
                            if (!method.isAccessible()) method.setAccessible(true);
                            return bind(MethodHandles.lookup().unreflect(method));
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw sanitizeStackTrace(new IllegalArgumentException("Cannot find method '" + name + "' in " + getHandleType()));
        }
        throw sanitizeStackTrace(new IllegalArgumentException("Cannot find method '" + name + "' in " + getHandleType()));
    }

    private Class<?> getHandleType() {
        if (handle instanceof Class)
            return (Class<?>) handle;
        return handle.getClass();
    }

    public static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    public static List<Method> getAllMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            methods.addAll(Arrays.asList(c.getDeclaredMethods()));
        }
        return methods;
    }

    private Mirror mirror(Method method) {
        try {
            String field = getFieldName(method);
            if (field != null) {
                Class<?> returnType = method.getReturnType();
                if (Mirror.class.isAssignableFrom(returnType)) {
                    MethodHandle getter = bind(MethodHandles.lookup().unreflectGetter(field(field)));
                    MirrorInvocationHandler invocationHandler = new MirrorInvocationHandler(getter.invokeWithArguments());
                    Mirror proxy = (Mirror) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{returnType}, invocationHandler);
                    invocationHandler.setMirror(proxy);
                    invocationHandler.setMirrorClass(returnType.getSimpleName());
                    return proxy;
                }
            }
            MethodHandle invoke = method(method.getName(), new Object[0]);
            MirrorInvocationHandler invocationHandler = new MirrorInvocationHandler(invoke.invokeWithArguments());
            Mirror proxy = (Mirror) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{method.getReturnType()}, invocationHandler);
            invocationHandler.setMirror(proxy);
            invocationHandler.setMirrorClass(method.getReturnType().getSimpleName());
            return proxy;
        } catch (Throwable e) {
            sneakyThrow(e);
            return null;
        }
    }

    private Object findEnum(Method method) {
        String name = getFieldName(method);
        Class enumClass = remap(method.getDeclaringClass(), method.getDeclaringClass());
        try {
            if (!enumClass.isEnum())
                throw new IllegalArgumentException("Class " + enumClass.getName() + " is not an enum type!");
            if (name == null) name = method.getName();
            Enum enumValue = Enum.valueOf(enumClass, name);
            Class<?> returnType = method.getReturnType();
            if (Mirror.class.isAssignableFrom(returnType)) {
                MirrorInvocationHandler invocationHandler = new MirrorInvocationHandler(enumValue);
                Mirror proxy = (Mirror) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{returnType}, invocationHandler);
                invocationHandler.setMirror(proxy);
                invocationHandler.setMirrorClass(returnType.getSimpleName());
                return proxy;
            } else {
                return enumValue;
            }
        } catch (Throwable t) {
            throw sanitizeStackTrace(new NoSuchElementException("Cannot find enum '" + name + "' in " + enumClass + " (requested by " + method.getName() + ")"));
        }
    }

    private String getFieldName(Method method) {
        MirrorField field = method.getAnnotation(MirrorField.class);
        if (field != null)
            return field.value();
        MirrorEnumName enumName = method.getAnnotation(MirrorEnumName.class);
        if (enumName != null)
            return enumName.value();
        ObfuscatedField obf = method.getAnnotation(ObfuscatedField.class);
        if (obf != null) {
            for (Mapping m : obf.value()) {
                if (m.version() == GameVersion.current())
                    return m.name();
            }
            if (!obf.defaultName().isEmpty())
                return obf.defaultName();
        }
        return null;
    }

    private String getMethodName(Method method) {
        MirrorMethod m = method.getAnnotation(MirrorMethod.class);
        if (m != null)
            return m.value();
        ObfuscatedMethod obf = method.getAnnotation(ObfuscatedMethod.class);
        if (obf != null) {
            for (Mapping mapping : obf.value()) {
                if (mapping.version() == GameVersion.current())
                    return mapping.name();
            }
            if (!obf.defaultName().isEmpty())
                return obf.defaultName();
        }
        return null;
    }

    private static Constructor<MethodHandles.Lookup> privateLookup;

    static {
        try {
            privateLookup = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            privateLookup.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    static void sneakyThrow(Throwable ex) {
        MirrorInvocationHandler.sneakyThrowInner(ex);
    }

    private static <T extends Throwable> void sneakyThrowInner(Throwable ex) throws T {
        throw (T) ex;
    }

    static <T extends Throwable> T sanitizeStackTrace(T throwable) {
        List<StackTraceElement> stackTrace = new ArrayList<>();
        Collections.addAll(stackTrace, throwable.getStackTrace());
        stackTrace.removeIf(t -> t.getClassName().contains("$Proxy")); // remove the "$ProxyX" paths because they are just useless
        stackTrace.removeIf(t -> t.getClassName().equals(MirrorInvocationHandler.class.getName())); // remove any traces to this class because it's not our fault
        stackTrace.removeIf(t -> t.getClassName().equals(MirrorFactory.class.getName())); // remove any traces to MirrorFactory because it's not our fault
        throwable.setStackTrace(stackTrace.toArray(new StackTraceElement[0]));
        return throwable;
    }

    private MethodHandle bind(MethodHandle handle) {
        if (!(this.handle instanceof Class))
            return handle.bindTo(this.handle);
        return handle;
    }

}
