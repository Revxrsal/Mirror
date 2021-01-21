package io.github.revxrsal.mirror;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a field which may have different names depending on the
 * version
 *
 * @see MirrorField
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ObfuscatedField {

    /**
     * The mappings for this field
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
