package io.github.revxrsal.mirror;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a mirrored object.
 */
public interface Mirror {

    /**
     * Returns the underlying handle that is being mirrored by this mirror.
     *
     * @return The mirror target
     */
    Object getMirrorTarget();

    /**
     * Returns the type of {@link Mirror#getMirrorTarget()}
     *
     * @return The mirrored type of this mirror
     */
    Class<?> getMirrorType();

    /**
     * Checks whether this mirror equals the other mirror
     *
     * @param other The other object. Must be a mirror to be equal
     * @return Whether are the 2 objects equal
     */
    boolean equals(Object other);

    /**
     * Returns the hash code of {@link Mirror#getMirrorTarget()}
     *
     * @return The hash code
     */
    int hashCode();

    /**
     * Returns the string of this mirror and handle
     *
     * @return The string for this mirror
     */
    String toString();

    /**
     * Creates a mirror wrapper for the specified handle
     *
     * @param handle     The object to "mirrorize".
     * @param mirrorType The mirror class.
     * @param <S>        The mirror generic
     * @return The mirror mirror.
     */
    static <S extends Mirror> S mirrorize(@NotNull Object handle, Class<S> mirrorType) {
        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(mirrorType, "mirrorType");
        return MirrorFactory.getInstance().wrap(handle, mirrorType);
    }

    /**
     * Constructs a handle from the mirror class. Arguments passed will automatically
     * get un-mirrorized.
     *
     * @param mirrorType The mirror class
     * @param args       Arguments to construct with. If any of it is a {@link Mirror}, it will be
     *                   un-mirrorized.
     * @param <S>        The mirror generic
     * @return The new mirror instance for the constructed handle.
     */
    static <S extends Mirror> S construct(@NotNull Class<S> mirrorType, Object... args) {
        Objects.requireNonNull(mirrorType, "mirrorType");
        return MirrorFactory.getInstance().construct(mirrorType, args);
    }

    /**
     * Returns a mirror for accessing static fields or methods in a class.
     * <p>
     * If any instance-method is accessed from that instance, an exception will
     * be thrown.
     *
     * @param mirrorType The mirror class
     * @param <S>        The mirror generic
     * @return The mirror static instance.
     */
    static <S extends Mirror> S forStatic(@NotNull Class<S> mirrorType) {
        Objects.requireNonNull(mirrorType, "mirrorType");
        return MirrorFactory.getInstance().createForStatic(mirrorType);
    }

    /**
     * Mirrorizes an enum class.
     *
     * @param mirrorType The mirror class
     * @param <S>        The mirror generic
     * @return The mirror mirror for the enum
     */
    static <S extends Mirror> S mirrorizeEnum(@NotNull Class<S> mirrorType) {
        Objects.requireNonNull(mirrorType);
        if (!mirrorType.isAnnotationPresent(MirrorEnum.class))
            throw new IllegalArgumentException(mirrorType + " must be annotated with @MirrorEnum!");
        return MirrorFactory.getInstance().mirrorEnum(mirrorType);
    }

}
