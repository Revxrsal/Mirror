package io.github.revxrsal.mirror;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.github.revxrsal.mirror.MirrorInvocationHandler.sanitizeStackTrace;
import static io.github.revxrsal.mirror.MirrorInvocationHandler.sneakyThrow;

/**
 * Represents a {@link MirrorClass} whose name may change depending on the
 * version
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VersionedMirrorClass {

    /**
     * The type of this class.
     *
     * @return The type
     * @see Type
     */
    Type type();

    /**
     * The mappings for this versioned class
     *
     * @return The mappings for the class name
     */
    Mapping[] value();

    /**
     * The default value, if none of the mapping rules is present
     *
     * @return The default name
     */
    String defaultName() default "";

    enum Type {

        /**
         * Represents a CraftBukkit class
         */
        CRAFTBUKKIT {
            @Override public Class<?> fetch( @NotNull String name) {
                return GameVersion.current().getCraftBukkit(name);
            }
        },

        /**
         * Represents an NMS class
         */
        NMS {
            @Override public Class<?> fetch( @NotNull String name) {
                return GameVersion.current().getNMS(name);
            }
        },

        /**
         * Represents a normal class
         */
        NONE {
            @Override public Class<?> fetch( @NotNull String name) {
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    sneakyThrow(sanitizeStackTrace(e));
                    return null;
                }
            }
        };

        /**
         * Fetches the class from the specified name
         * @param name
         * @return
         */
        public abstract Class<?> fetch( @NotNull String name);

    }

}
