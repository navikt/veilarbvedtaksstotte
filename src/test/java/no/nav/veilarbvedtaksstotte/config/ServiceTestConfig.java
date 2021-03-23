package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.service.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        ArenaVedtakService.class,
        AuthService.class,
        BeslutterService.class,
        BeslutteroversiktService.class,
        MalTypeService.class,
        MeldingService.class,
        MetricsService.class,
        OyeblikksbildeService.class,
        VedtakService.class,
        VedtakStatusEndringService.class,
        VeilederService.class,
        DokumentServiceV2.class,
        UnleashService.class
})
public class ServiceTestConfig {

    @Bean
    public KafkaProducerService kafkaProducerService() {
        return mock(KafkaProducerService.class);
    }
}
