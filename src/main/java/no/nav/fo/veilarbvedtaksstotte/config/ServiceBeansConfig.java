package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.services.VeilederService;
import org.springframework.context.annotation.Bean;

@Slf4j
public class ServiceBeansConfig {

    @Bean
    public VeilederService veilederService() {
        return new VeilederService();
    }

}
