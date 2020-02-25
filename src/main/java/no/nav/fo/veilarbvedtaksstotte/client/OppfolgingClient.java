package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.OppfolgingDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.OppfolgingstatusDTO;
import org.springframework.cache.annotation.Cacheable;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.fo.veilarbvedtaksstotte.config.CacheConfig.OPPFOLGING_CACHE_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class OppfolgingClient extends BaseClient {
    public static final String VEILARBOPPFOLGING_API_PROPERTY_NAME = "VEILARBOPPFOLGINGAPI_URL";
    public static final String VEILARBOPPFOLGING = "veilarboppfolging";

    public OppfolgingClient() {
        super(getRequiredProperty(VEILARBOPPFOLGING_API_PROPERTY_NAME));
    }

    public String hentServicegruppe(String fnr) {
        return get(joinPaths(baseUrl, "api", "person", fnr, "oppfolgingsstatus"), OppfolgingstatusDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarboppfolging/api/person/{fnr}/oppfolgingsstatus"))
                .getServicegruppe();
    }

    @Cacheable(OPPFOLGING_CACHE_NAME)
    public OppfolgingDTO hentOppfolgingData(String fnr) {
        return get((joinPaths(baseUrl, "api", "oppfolging?fnr=") + fnr), OppfolgingDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarboppfolging/api/oppfolging?fnr"));
    }
}
