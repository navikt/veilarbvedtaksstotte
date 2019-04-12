package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static no.nav.sbl.rest.RestUtils.RestConfig.builder;
import static no.nav.sbl.rest.RestUtils.withClient;

@Slf4j
public class BaseClient {

    protected final String baseUrl;
    protected final Provider<HttpServletRequest> httpServletRequestProvider;
    protected static final int HTTP_READ_TIMEOUT = 120000;

    BaseClient(String baseUrl, Provider<HttpServletRequest> httpServletRequestProvider) {
        this.baseUrl = baseUrl;
        this.httpServletRequestProvider = httpServletRequestProvider;
    }

    protected <T> T postWithClient(String url, Object postData, Class<T> returnedDataClass) {
        return withClient(
                builder().readTimeout(HTTP_READ_TIMEOUT).build(),
                c -> post(c, url, postData, returnedDataClass)
        );
    }

    private void sjekkHttpResponse(Response response, String url) {
        int status = response.getStatus();

        if (status >= 400) {
            throw new RuntimeException("Uventet respons (" + status + ") ved kall mot mot " + url);
        }
    }

    private <T> T post(Client client, String url, Object postData, Class<T> returnedDataClass) {
        String cookies = httpServletRequestProvider.get().getHeader(COOKIE);

        Response response = client.target(url)
                .request()
                .header(COOKIE, cookies)
                .post(json(postData));

        sjekkHttpResponse(response, url);

        return response.readEntity(returnedDataClass);
    }

}
