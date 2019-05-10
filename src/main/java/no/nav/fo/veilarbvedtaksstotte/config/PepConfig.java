package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static java.lang.System.getProperty;

@Configuration
@Import({AbacContext.class})
public class PepConfig {

    public static final String VEILARBABAC_API_URL_PROPERTY = "VEILARBABAC_API_URL";
    public static final String VEILARBABAC = "veilarbabac";

    private SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();

    @Bean
    public VeilarbAbacPepClient pepClient(Pep pep) {

        return VeilarbAbacPepClient.ny()
                .medPep(pep)
                .medSystemUserTokenProvider(() -> systemUserTokenProvider.getToken())
                .brukAktoerId(() -> true)
                .sammenlikneTilgang(() -> false)
                .foretrekkVeilarbAbacResultat(() -> true)
                .medVeilarbAbacUrl(getProperty(VEILARBABAC_API_URL_PROPERTY))
                .bygg();
    }

}
