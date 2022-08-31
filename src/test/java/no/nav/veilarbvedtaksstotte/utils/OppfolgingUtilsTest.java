package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.ZonedDateTime.now;
import static org.junit.Assert.*;

public class OppfolgingUtilsTest {

    @Test
    public void hentSisteOppfolgingsPeriode__skal_hente_siste_periode() {
        OppfolgingPeriodeDTO periode1 = periode(now(), now().plusDays(7));

        OppfolgingPeriodeDTO periode2 = periode(now().plusDays(8), now().plusDays(10));

        List<OppfolgingPeriodeDTO> oppfolgingPerioder = List.of(periode1, periode2);

        Optional<OppfolgingPeriodeDTO> maybePeriode = OppfolgingUtils.hentSisteOppfolgingsPeriode(oppfolgingPerioder);
        assertTrue(maybePeriode.isPresent());
        assertEquals(periode2, maybePeriode.get());
    }

    @Test
    public void hentSisteOppfolgingsPeriode__skal_hente_siste_periode_hvis_mangler_sluttdato() {
        OppfolgingPeriodeDTO periode1 = periode(now(), now().plusDays(7));
        OppfolgingPeriodeDTO periode2 = periode(now().plusDays(8), null);

        List<OppfolgingPeriodeDTO> oppfolgingPerioder = List.of(periode1, periode2);

        Optional<OppfolgingPeriodeDTO> maybePeriode = OppfolgingUtils.hentSisteOppfolgingsPeriode(oppfolgingPerioder);
        assertTrue(maybePeriode.isPresent());
        assertEquals(periode2, maybePeriode.get());
    }

    private OppfolgingPeriodeDTO periode(ZonedDateTime start, ZonedDateTime slutt) {
        OppfolgingPeriodeDTO periode = new OppfolgingPeriodeDTO();
        periode.setStartDato(start);
        periode.setSluttDato(slutt);
        return periode;
    }
}
