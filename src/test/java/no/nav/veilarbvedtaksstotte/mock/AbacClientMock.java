package no.nav.veilarbvedtaksstotte.mock;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;

public class AbacClientMock implements AbacClient {
    @Override
    public String sendRawRequest(String s) {
        return null;
    }

    @Override
    public XacmlResponse sendRequest(XacmlRequest xacmlRequest) {
        return null;
    }
}
