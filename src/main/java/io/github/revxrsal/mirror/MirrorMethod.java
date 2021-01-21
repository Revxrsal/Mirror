package io.github.revxrsal.mirror;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a mirrored method which has a fully known and consistent name.
 *
 * @see ObfuscatedMethod
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MirrorMethod {

    /**
     * The method name
     *
     * @return The method name
     */
    String value();

}
