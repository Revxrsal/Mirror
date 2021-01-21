package io.github.revxrsal.mirror;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a net.minecraft.server class
 *
 * @see OcbClass
 * @see MirrorClass
 * @see VersionedMirrorClass
 */
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NmsClass {

    /**
     * The class name, after "net.minecraft.server.(version)."
     *
     * @return The class name
     */
    String value();

}
