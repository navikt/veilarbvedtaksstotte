package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.mock.AktorServiceMock;
import no.nav.fo.veilarbvedtaksstotte.mock.PepClientMock;
import no.nav.fo.veilarbvedtaksstotte.service.*;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        ResourceConfig.class,
        DatabaseTestConfig.class,
        ClientConfig.class,
        CacheConfig.class,
        RepositoryConfig.class,
        VeilederService.class,
        MalTypeService.class,
        VedtakService.class,
        AuthService.class,
        MetricsService.class,
        BeslutterOppgaveService.class
})
public class TestConfigs {

    @Bean
    public VeilarbAbacPepClient veilarbAbacPepClient() {
        return mock(VeilarbAbacPepClient.class);
    }

    @Bean
    public KafkaService kafkaService() {
        return mock(KafkaService.class);
    }

    @Bean
    public UnleashService unleashService() {
        return mock(UnleashService.class);
    }

    @Bean
    public AktorService aktorService() {
        return new AktorServiceMock();
    }

    @Bean
    public AktoerV2 aktoerV2() {
        return mock(AktoerV2.class);
    }

    @Bean
    public PepClient pepClient() {
        return new PepClientMock();
    }

}
