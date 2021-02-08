package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OppfolgingUtilsTest {

    @Test
    public void hentSisteOppfolgingsPeriode__skal_hente_siste_periode() {
        OppfolgingPeriodeDTO periode1 = new OppfolgingPeriodeDTO();
        periode1.setStartDato(LocalDateTime.now());
        periode1.setSluttDato(LocalDateTime.now().plusDays(7));

        OppfolgingPeriodeDTO periode2 = new OppfolgingPeriodeDTO();
        periode2.setStartDato(LocalDateTime.now().plusDays(8));
        periode2.setSluttDato(LocalDateTime.now().plusDays(10));

        List<OppfolgingPeriodeDTO> oppfolgingPerioder = List.of(periode1, periode2);

        Optional<OppfolgingPeriodeDTO> maybePeriode = OppfolgingUtils.hentSisteOppfolgingsPeriode(oppfolgingPerioder);
        assertTrue(maybePeriode.isPresent());
        assertEquals(periode2, maybePeriode.get());
    }

    @Test
    public void hentSisteOppfolgingsPeriode__skal_hente_siste_periode_hvis_mangler_sluttdato() {
        OppfolgingPeriodeDTO periode1 = new OppfolgingPeriodeDTO();
        periode1.setStartDato(LocalDateTime.now());
        periode1.setSluttDato(LocalDateTime.now().plusDays(7));

        OppfolgingPeriodeDTO periode2 = new OppfolgingPeriodeDTO();
        periode2.setStartDato(LocalDateTime.now().plusDays(8));
        periode2.setSluttDato(null);

        List<OppfolgingPeriodeDTO> oppfolgingPerioder = List.of(periode1, periode2);

        Optional<OppfolgingPeriodeDTO> maybePeriode = OppfolgingUtils.hentSisteOppfolgingsPeriode(oppfolgingPerioder);
        assertTrue(maybePeriode.isPresent());
        assertEquals(periode2, maybePeriode.get());
    }

}
