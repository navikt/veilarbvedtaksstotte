package no.nav.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.EnhetNavn;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import no.nav.veilarbvedtaksstotte.domain.VeilederePaEnhetDTO;
import no.nav.veilarbvedtaksstotte.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class VeiledereOgEnhetClient extends BaseClient {

    public static final String VEILARBVEILEDER_API_PROPERTY_NAME = "VEILARBVEILEDERAPI_URL";
    public static final String VEILARBVEILEDER = "veilarbveileder";

    public VeiledereOgEnhetClient() {
        super(getRequiredProperty(VEILARBVEILEDER_API_PROPERTY_NAME));
    }

    @Cacheable(CacheConfig.ENHET_NAVN_CACHE_NAME)
    public String hentEnhetNavn(String enhetId) {
        return get(joinPaths(baseUrl, "api", "enhet", enhetId, "navn"), EnhetNavn.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarbveileder/api/enhet/{enhetId}/navn"))
                .getNavn();
    }

    @Cacheable(CacheConfig.VEILEDER_CACHE_NAME)
    public Veileder hentVeileder(String veilederIdent) {
        return get(joinPaths(baseUrl, "api", "veileder", veilederIdent), Veileder.class)
                .withStatusCheck()
                .getData()
                .orElse(null);
    }

}

