package no.nav.veilarbvedtaksstotte.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.VedtakUtils.erKilderLike;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VedtakUtilsTest {

    @Test
    public void erKilderLike__skal_sjekke_at_kilder_er_like() {
        assertTrue(erKilderLike(List.of("test1"), List.of("test1")));
        assertTrue(erKilderLike(List.of("test1", "test2"), List.of("test1", "test2")));
    }

    @Test
    public void erKilderLike__skal_sjekke_at_kilder_er_ulike() {
        assertFalse(erKilderLike(List.of("test1", "test2"), List.of("test2", "test1")));

        List<String> kilder = new ArrayList<>();
        kilder.add("test1");
        kilder.add(null);
        assertFalse(erKilderLike(kilder, List.of("test1", "test2")));
    }

}
