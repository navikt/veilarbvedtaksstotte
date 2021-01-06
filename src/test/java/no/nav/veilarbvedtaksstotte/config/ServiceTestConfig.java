package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.service.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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
        DokumentServiceV2.class
})
public class ServiceTestConfig {}
