package io.github.revxrsal.mirror;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a method which may have different names depending on the
 * version
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ObfuscatedMethod {

    /**
     * The mappings for this method
     *
     * @return The mappings
     */
    Mapping[] value();

    /**
     * The default name, if none of the mapping rules is present
     *
     * @return The default name
     */
    String defaultName() default "";

}
