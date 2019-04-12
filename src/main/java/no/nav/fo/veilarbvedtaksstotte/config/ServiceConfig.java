package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.service.VeilederService;
import org.springframework.context.annotation.Bean;

@Slf4j
public class ServiceConfig {

    @Bean
    public VeilederService veilederService() {
        return new VeilederService();
    }

}
