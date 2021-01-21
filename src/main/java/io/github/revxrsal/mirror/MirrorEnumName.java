package io.github.revxrsal.mirror;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents the name of an enumeration constant. Must only be used
 * in classes annotated with {@link MirrorEnum}.
 * <p>
 * Methods annotated with this will return the corresponding enum constant
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MirrorEnumName {

    /**
     * The constant name
     *
     * @return The enum constant
     */
    String value();

}
