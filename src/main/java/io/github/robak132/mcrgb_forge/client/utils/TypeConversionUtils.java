package io.github.robak132.mcrgb_forge.client.utils;

public abstract class TypeConversionUtils {

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
