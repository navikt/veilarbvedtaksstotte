package no.nav.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.OppfolgingDTO;
import no.nav.veilarbvedtaksstotte.domain.OppfolgingstatusDTO;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class OppfolgingClient extends BaseClient {
    public static final String VEILARBOPPFOLGING_API_PROPERTY_NAME = "VEILARBOPPFOLGINGAPI_URL";
    public static final String VEILARBOPPFOLGING = "veilarboppfolging";

    public OppfolgingClient() {
        super(getRequiredProperty(VEILARBOPPFOLGING_API_PROPERTY_NAME));
    }

    public String hentServicegruppe(String fnr) {
        return get(String.join("", baseUrl, "api", "person", fnr, "oppfolgingsstatus"), OppfolgingstatusDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarboppfolging/api/person/{fnr}/oppfolgingsstatus"))
                .getServicegruppe();
    }

//    @Cacheable(CacheConfig.OPPFOLGING_CACHE_NAME)
    public OppfolgingDTO hentOppfolgingData(String fnr) {
        return get((joinPaths(baseUrl, "api", "oppfolging?fnr=") + fnr), OppfolgingDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarboppfolging/api/oppfolging?fnr"));
    }
}
