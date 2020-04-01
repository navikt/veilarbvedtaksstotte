package no.nav.veilarbvedtaksstotte.utils;

import java.util.List;

public class ValidationUtils {

    public static boolean isListEmpty(List list) {
        return list == null || list.isEmpty();
    }

    public static boolean isStringBlank(String str) {
        return str == null || str.isBlank();
    }

}
