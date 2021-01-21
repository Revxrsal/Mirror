package io.github.revxrsal.mirror;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a mirrored field
 *
 * @see ObfuscatedField
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MirrorField {

    /**
     * The field name
     *
     * @return The field name
     */
    String value();

}
