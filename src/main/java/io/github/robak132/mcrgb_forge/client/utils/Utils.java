package io.github.robak132.mcrgb_forge.client.utils;

public abstract class Utils {
    private Utils() {}

    public static long elapsedMillis(long started) {
        return nanosToMillis(System.nanoTime() - started);
    }

    public static long nanosToMillis(long nanos) {
        return nanos / 1_000_000;
    }

    public static long perSecond(long completed, long elapsedNanos) {
        return elapsedNanos == 0 ? 0 : completed * 1_000_000_000L / elapsedNanos;
    }

    public static long usedMemoryMib() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    public static long maximumMemoryMib() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    public static Integer hexToInt(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer stringToInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
