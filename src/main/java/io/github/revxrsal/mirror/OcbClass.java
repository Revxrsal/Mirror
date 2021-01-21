package io.github.revxrsal.mirror;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a CraftBukkit class.
 *
 * @see NmsClass
 * @see VersionedMirrorClass
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.METHOD})
public @interface OcbClass {

    /**
     * The class name, coming after "org.craftbukkit.(version)."
     *
     * @return The class name
     */
    String value();

}
