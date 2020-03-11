package no.nav.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class EgenvurderingClient extends BaseClient {

    public static final String EGENVURDERING_API_PROPERTY_NAME = "EGENVURDERING_URL";
    public static final String VEILARBVEDTAKINFO = "veilarbvedtakinfo";

    public EgenvurderingClient() {
        super(getRequiredProperty(EGENVURDERING_API_PROPERTY_NAME));
    }

    public String hentEgenvurdering(String fnr) {
        RestResponse<String> response = get(joinPaths(baseUrl, "api", "behovsvurdering", "besvarelse?fnr=") + fnr, String.class);

        if (response.hasStatus(204)) {
            return JsonUtils.createNoDataStr("Bruker har ikke fylt ut egenvurdering");
        }

        return response.withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot veilarbvedtakinfo/api/behovsvurdering/besvarelse"));
    }
}
