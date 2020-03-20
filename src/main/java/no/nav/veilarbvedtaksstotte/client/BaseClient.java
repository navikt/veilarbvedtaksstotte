package no.nav.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.veilarbvedtaksstotte.utils.AutentiseringUtils.createBearerToken;
import static no.nav.sbl.rest.RestUtils.RestConfig.builder;
import static no.nav.sbl.rest.RestUtils.withClient;

@Slf4j
public class BaseClient {

    protected final String baseUrl;
    private static final int HTTP_READ_TIMEOUT = 2 * 60 * 1000; // two minutes

    BaseClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    protected <T> RestResponse<T> post(String url, Object postData, Class<T> returnedDataClass) {
        return withClient(
                builder().readTimeout(HTTP_READ_TIMEOUT).build(),
                c -> post(c, url, postData, returnedDataClass)
        );
    }

    protected <T> RestResponse<T> get(String url, Class<T> returnedDataClass) {
        return withClient(
                builder().readTimeout(HTTP_READ_TIMEOUT).build(),
                c -> get(c, url, returnedDataClass)
        );
    }

    private <T> RestResponse<T> get(Client client, String url, Class<T> returnedDataClass) {
        Response response = client.target(url)
                .request()
                .header(AUTHORIZATION, createBearerToken())
                .get();

        return new RestResponse<>(response, returnedDataClass, url);
    }

    private <T> RestResponse<T> post(Client client, String url, Object postData, Class<T> returnedDataClass) {
        Response response = client.target(url)
                .request()
                .header(AUTHORIZATION, createBearerToken())
                .post(json(postData));

        return new RestResponse<>(response, returnedDataClass, url);
    }


    public class RestResponse<T> {

        private final Response response;

        private final String url;

        private final T data;

        public RestResponse(Response response, Class<T> dataClass, String url){
            this.response = response;
            this.url = url;
            data = getData(response, dataClass);
        }

        public int getStatus() {
            return response.getStatus();
        }

        public boolean hasStatus (int status) {
            return getStatus() == status;
        }

        public RestResponse<T> withStatusCheck() {
            int status = response.getStatus();

            if (status >= 400) {
                throw new RuntimeException(format("Uventet respons (%d) ved kall mot mot %s", status, url));
            }

            return this;
        }

        public Optional<T> getData() {
            return Optional.ofNullable(data);
        }

        private T getData(Response response, Class<T> responseDataClass) {
            try {
                return response.readEntity(responseDataClass);
            }catch (Exception e) {
                return null;
            }
        }

    }

}