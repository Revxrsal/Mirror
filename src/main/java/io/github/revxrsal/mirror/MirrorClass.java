package io.github.revxrsal.mirror;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a mirror for a class
 *
 * @see OcbClass
 * @see NmsClass
 * @see VersionedMirrorClass
 */
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MirrorClass {

    String value();

}
