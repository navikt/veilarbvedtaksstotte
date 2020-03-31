package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.service.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        VeilederService.class,
        MalTypeService.class,
        VedtakService.class,
		OyeblikksbildeService.class,
        KafkaService.class,
        AuthService.class,
        MetricsService.class,
		ArenaVedtakService.class,
		BeslutterService.class,
		DialogService.class,
		VedtakStatusEndringService.class,
		BeslutteroversiktService.class
})
public class ServiceConfig {}
