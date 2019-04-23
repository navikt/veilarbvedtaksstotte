package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.PersonNavn;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class PersonClient extends BaseClient {

    public static final String PERSON_API_PROPERTY_NAME = "VEILARBPERSONAPI_URL";
    public static final String VEILARBPERSON = "veilarbperson";

    @Inject
    public PersonClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(PERSON_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public PersonNavn hentNavn(String fnr) {
        return get(joinPaths(baseUrl, "api", "person", "navn?fnr=" + fnr), PersonNavn.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot veilarbperson/navn"));
    }

}
