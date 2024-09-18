package no.nav.veilarbvedtaksstotte.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class DbUtils {
    public static String toPostgresArray(List<String> values) {
        return "{" + String.join(",", values) + "}";
    }

    public static <T> T queryForObjectOrNull(Supplier<T> query) {
        try {
            return query.get();
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

}
