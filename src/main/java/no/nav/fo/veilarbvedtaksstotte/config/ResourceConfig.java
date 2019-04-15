package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.resource.VedtakResource;
import no.nav.fo.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.context.annotation.Bean;

@Slf4j
public class ResourceConfig {

    @Bean
    public VedtakResource vedtakResource(VedtakService vedtakService) {
        return new VedtakResource(vedtakService);
    }

}
