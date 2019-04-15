package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.client.DokumentClient;
import no.nav.fo.veilarbvedtaksstotte.client.ModiaContextClient;
import no.nav.fo.veilarbvedtaksstotte.client.PersonClient;
import org.springframework.context.annotation.Bean;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

@Slf4j
public class ClientConfig {

    @Bean
    public DokumentClient dokumentClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        return new DokumentClient(httpServletRequestProvider);
    }

    @Bean
    public PersonClient personClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        return new PersonClient(httpServletRequestProvider);
    }

    @Bean
    public ModiaContextClient modiaContextClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        return new ModiaContextClient(httpServletRequestProvider);
    }

}
