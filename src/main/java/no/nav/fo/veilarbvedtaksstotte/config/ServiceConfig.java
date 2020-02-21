package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.fo.veilarbvedtaksstotte.service.*;
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
		BeslutterOppgaveService.class
})
public class ServiceConfig {}
