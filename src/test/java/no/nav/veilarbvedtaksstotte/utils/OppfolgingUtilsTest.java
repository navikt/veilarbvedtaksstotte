package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class OppfolgingUtilsTest {

    @Test
    public void hentSisteOppfolgingsPeriode__skal_hente_siste_periode() {
        OppfolgingPeriodeDTO periode1 = periode(LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        OppfolgingPeriodeDTO periode2 = periode(LocalDateTime.now().plusDays(8), LocalDateTime.now().plusDays(10));

        List<OppfolgingPeriodeDTO> oppfolgingPerioder = List.of(periode1, periode2);

        Optional<OppfolgingPeriodeDTO> maybePeriode = OppfolgingUtils.hentSisteOppfolgingsPeriode(oppfolgingPerioder);
        assertTrue(maybePeriode.isPresent());
        assertEquals(periode2, maybePeriode.get());
    }

    @Test
    public void hentSisteOppfolgingsPeriode__skal_hente_siste_periode_hvis_mangler_sluttdato() {
        OppfolgingPeriodeDTO periode1 = periode(LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        OppfolgingPeriodeDTO periode2 = periode(LocalDateTime.now().plusDays(8), null);

        List<OppfolgingPeriodeDTO> oppfolgingPerioder = List.of(periode1, periode2);

        Optional<OppfolgingPeriodeDTO> maybePeriode = OppfolgingUtils.hentSisteOppfolgingsPeriode(oppfolgingPerioder);
        assertTrue(maybePeriode.isPresent());
        assertEquals(periode2, maybePeriode.get());
    }


    @Test
    public void erDatoInnenforOppfolgingsperiode__riktig_svar_for_om_dato_er_innenfor_periode() {
        LocalDateTime now = LocalDateTime.now();

        assertTrue("innenfor periode",
                OppfolgingUtils.erDatoInnenforOppfolgingsperiode(now, periode(now.minusDays(1), now)));

        assertTrue("innenfor når dato er lik startdato",
                OppfolgingUtils.erDatoInnenforOppfolgingsperiode(now, periode(now, now.plusDays(1))));
        assertTrue("innenfor når dato er lik sluttdato",
                OppfolgingUtils.erDatoInnenforOppfolgingsperiode(now, periode(now.minusDays(1), now)));

        assertFalse("utenfor når dato er før startdato",
                OppfolgingUtils.erDatoInnenforOppfolgingsperiode(now.minusDays(1).minusSeconds(1), periode(now.minusDays(1), now)));
        assertFalse("utenfor når dato er etter sluttdato",
                OppfolgingUtils.erDatoInnenforOppfolgingsperiode(now.plusDays(1).plusSeconds(1), periode(now, now.plusDays(1))));

        assertTrue("innenfor når dato er etter startdato og sluttdato er null",
                OppfolgingUtils.erDatoInnenforOppfolgingsperiode(now, periode(now.minusDays(1), null)));
        assertFalse("utenfor når dato er før startdato og sluttdato er null",
                OppfolgingUtils.erDatoInnenforOppfolgingsperiode(now.minusDays(1).minusSeconds(1), periode(now.minusDays(1), null)));
    }

    private OppfolgingPeriodeDTO periode(LocalDateTime start, LocalDateTime slutt) {
        OppfolgingPeriodeDTO periode = new OppfolgingPeriodeDTO();
        periode.setStartDato(start);
        periode.setSluttDato(slutt);
        return periode;
    }
}
