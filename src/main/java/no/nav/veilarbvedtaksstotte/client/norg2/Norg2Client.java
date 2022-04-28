package no.nav.veilarbvedtaksstotte.client.norg2;

import no.nav.common.client.norg2.Enhet;
import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.EnhetId;

import java.util.List;

public interface Norg2Client extends HealthCheck {

    Enhet hentEnhet(String enhetId);

    List<Enhet> hentAktiveEnheter();

    EnhetKontaktinformasjon hentKontaktinfo(EnhetId enhetId);

    List<EnhetOrganisering> hentEnhetOrganisering(EnhetId enhetId);

}
