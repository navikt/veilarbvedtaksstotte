package no.nav.veilarbvedtaksstotte.utils;

import java.util.List;

public class ValidationUtils {

    public static boolean isNullOrEmpty(List list) {
        return list == null || list.isEmpty();
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isBlank();
    }

}
