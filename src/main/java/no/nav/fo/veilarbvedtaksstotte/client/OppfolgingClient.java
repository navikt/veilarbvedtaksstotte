package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.OppfolgingDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.OppfolgingPeriodeDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.OppfolgingstatusDTO;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class OppfolgingClient extends BaseClient {
    public static final String VEILARBOPPFOLGING_API_PROPERTY_NAME = "VEILARBOPPFOLGINGAPI_URL";
    public static final String VEILARBOPPFOLGING = "veilarboppfolging";

    @Inject
    public OppfolgingClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(VEILARBOPPFOLGING_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public String hentServicegruppe(String fnr) {
        return get(joinPaths(baseUrl, "api", "person", fnr, "oppfolgingsstatus"), OppfolgingstatusDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarboppfolging/api/person/{fnr}/oppfolgingsstatus"))
                .getServicegruppe();
    }

    public List<OppfolgingPeriodeDTO> hentOppfolgingsPerioder(String fnr) {
        return get((joinPaths(baseUrl, "api", "oppfolging?fnr=") + fnr), OppfolgingDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarboppfolging/api/oppfolging?fnr"))
                .getOppfolgingsPerioder();
    }
}