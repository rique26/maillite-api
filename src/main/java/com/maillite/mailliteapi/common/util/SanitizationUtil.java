package com.maillite.mailliteapi.common.util;

public final class SanitizationUtil {

    private SanitizationUtil() {}

    public static String email(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    public static String removeNonDigits(String value) {
        return value != null ? value.replaceAll("\\D", "") : "";
    }

    public static String optionalText(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }
}