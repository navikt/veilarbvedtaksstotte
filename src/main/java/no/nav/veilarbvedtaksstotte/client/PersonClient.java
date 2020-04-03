package no.nav.veilarbvedtaksstotte.client;

import no.nav.veilarbvedtaksstotte.domain.PersonNavn;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class PersonClient extends BaseClient {

    public static final String VEILARBPERSON_API_PROPERTY_NAME = "VEILARPERSONAPI_URL";
    public static final String VEILARBPERSON = "veilarbperson";

    public PersonClient() {
        super(getRequiredProperty(VEILARBPERSON_API_PROPERTY_NAME));
    }

    public PersonNavn hentPersonNavn(String fnr) {
        return get((joinPaths(baseUrl, "api", "person", "navn?fnr=") + fnr), PersonNavn.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot /veilarbperson/api/person/navn?fnr"));
    }

}
