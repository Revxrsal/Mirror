package io.github.revxrsal.mirror;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a version mapping
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Mapping {

    /**
     * Represents the version this mapping corresponds to
     *
     * @return The version
     */
    GameVersion version();

    /**
     * Represents the specific name in the version
     *
     * @return The name
     */
    String name();

}
