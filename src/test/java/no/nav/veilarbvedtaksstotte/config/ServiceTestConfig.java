package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ArbeidssoekerRegisteretService;
import no.nav.veilarbvedtaksstotte.controller.AuditlogService;
import no.nav.veilarbvedtaksstotte.service.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        ArenaVedtakService.class,
        AuthService.class,
        BeslutterService.class,
        BeslutteroversiktService.class,
        EnhetInfoService.class,
        MalTypeService.class,
        MeldingService.class,
        MetricsService.class,
        OyeblikksbildeService.class,
        VedtakService.class,
        VedtakHendelserService.class,
        VeilarbarenaService.class,
        VeilederService.class,
        DokumentService.class,
        DistribusjonService.class,
        UtrullingService.class,
        Siste14aVedtakService.class,
        KafkaProducerService.class,
        KafkaConsumerService.class,
        KafkaRepubliseringService.class,
        DvhRapporteringService.class,
        DvhRapporteringService.class,
        KafkaVedtakStatusEndringConsumer.class,
        BigQueryService.class,
        SakStatistikkService.class,
        SakStatistikkResendingService.class,
        ArbeidssoekerRegisteretService.class,
        PdfService.class,
        Gjeldende14aVedtakService.class,
        AuditlogService.class
})
public class ServiceTestConfig {
}
