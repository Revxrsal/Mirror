package io.github.revxrsal.mirror;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.github.revxrsal.mirror.MirrorInvocationHandler.sanitizeStackTrace;
import static io.github.revxrsal.mirror.MirrorInvocationHandler.sneakyThrow;

/**
 * A utility for accessing game versions easily
 */
public enum GameVersion {

    NONE,
    v1_8_R1,
    v1_8_R2,
    v1_8_R3,
    v1_9_R1,
    v1_9_R2,
    v1_10_R1,
    v1_11_R2,
    v1_12_R1,
    v1_13_R1,
    v1_13_R2,
    v1_14_R1,
    v1_15_R1,
    v1_16_R1,
    v1_16_R2,
    v1_16_R3,

    // just predictions
    v1_17_R1,
    v1_17_R2;

    private final String version;

    GameVersion() {
        version = "." + name() + ".";
    }

    /**
     * Returns the current game version
     *
     * @return The current game version
     */
    public static @NotNull GameVersion current() {
        return CURRENT;
    }

    /**
     * Returns the {@link GameVersion} corresponding with
     * the given string.
     *
     * @param name The version
     * @return The game version
     */
    public static GameVersion wrap(@NotNull String name) {
        return BY_VERSION.get(name);
    }

    /**
     * Returns the version of this {@link GameVersion}
     *
     * @return The protocol version
     */
    public @NotNull String getVersion() {
        return name();
    }

    /**
     * Returns the NMS class corresponding
     *
     * @param name The name of the class
     * @return The NMS class
     * @throws IllegalArgumentException if the class is invalid.
     */
    public Class<?> getNMS(@NotNull String name) {
        try {
            return Class.forName(this == NONE ? "net.minecraft.server." + name : "net.minecraft.server" + current().version + name);
        } catch (ClassNotFoundException e) {
            sneakyThrow(sanitizeStackTrace(e));
            return null;
        }
    }

    /**
     * Returns the CraftBukkit class corresponding
     *
     * @param name The name of the class
     * @return The CraftBukkit class
     * @throws IllegalArgumentException if the class is invalid.
     */
    public Class<?> getCraftBukkit(@NotNull String name) {
        try {
            return Class.forName(this == NONE ? "org.bukkit.craftbukkit." + name : "org.bukkit.craftbukkit" + current().version + name);
        } catch (ClassNotFoundException e) {
            sneakyThrow(sanitizeStackTrace(e));
            return null;
        }
    }

    public static boolean isBukkitEnvironment() {
        return current() != NONE;
    }

    private static final Map<String, GameVersion> BY_VERSION;
    private static final GameVersion CURRENT;

    static {
        Map<String, GameVersion> byVersion = new HashMap<>();
        for (GameVersion version : values()) {
            byVersion.put(version.name(), version);
        }
        BY_VERSION = Collections.unmodifiableMap(byVersion);
        GameVersion current = NONE;
        try {
            String a = Bukkit.getServer().getClass().getPackage().getName();
            String version = a.substring(a.lastIndexOf('.') + 1);
            current = BY_VERSION.get(version);
        } catch (Throwable ignored) {
            // we're not in a Bukkit environment
        }
        CURRENT = current;
    }

}
