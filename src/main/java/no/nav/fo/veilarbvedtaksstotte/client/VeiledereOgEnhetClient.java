package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.EnhetNavn;
import no.nav.fo.veilarbvedtaksstotte.domain.Veileder;
import no.nav.fo.veilarbvedtaksstotte.domain.VeilederePaEnhetDTO;
import org.springframework.cache.annotation.Cacheable;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.fo.veilarbvedtaksstotte.config.CacheConfig.ENHET_NAVN_CACHE_NAME;
import static no.nav.fo.veilarbvedtaksstotte.config.CacheConfig.VEILEDER_CACHE_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class VeiledereOgEnhetClient extends BaseClient {

    public static final String VEILARBVEILEDER_API_PROPERTY_NAME = "VEILARBVEILEDERAPI_URL";
    public static final String VEILARBVEILEDER = "veilarbveileder";

    public VeiledereOgEnhetClient() {
        super(getRequiredProperty(VEILARBVEILEDER_API_PROPERTY_NAME));
    }

    @Cacheable(ENHET_NAVN_CACHE_NAME)
    public String hentEnhetNavn(String enhetId) {
        return get(joinPaths(baseUrl, "api", "enhet", enhetId, "navn"), EnhetNavn.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarbveileder/api/enhet/{enhetId}/navn"))
                .getNavn();
    }

    public VeilederePaEnhetDTO hentVeilederePaEnhet(String enhetId) {
        return get(joinPaths(baseUrl, "api", "enhet", enhetId, "veiledere"), VeilederePaEnhetDTO.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarbveileder/api/enhet/{enhetId}/veiledere"));
    }

    @Cacheable(VEILEDER_CACHE_NAME)
    public Veileder hentVeileder(String veilederIdent) {
        return get(joinPaths(baseUrl, "api", "veileder", veilederIdent), Veileder.class)
                .withStatusCheck()
                .getData()
                .orElse(null);
    }

}

