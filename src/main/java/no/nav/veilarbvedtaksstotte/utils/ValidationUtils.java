package no.nav.veilarbvedtaksstotte.utils;

import java.util.List;

public class ValidationUtils {

    public static <T> boolean isNull(T anyObject) {
        return anyObject == null;
    }

    public static boolean isNullOrEmpty(List list) {
        return isNull(list) || list.isEmpty();
    }

    public static boolean isNullOrEmpty(String str) {
        return isNull(str) || str.trim().isEmpty();
    }

}
