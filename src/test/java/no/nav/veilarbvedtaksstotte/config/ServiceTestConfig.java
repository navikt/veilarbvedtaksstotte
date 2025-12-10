package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.controller.AuditlogService;
import no.nav.veilarbvedtaksstotte.service.ArenaVedtakService;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import no.nav.veilarbvedtaksstotte.service.BeslutterService;
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService;
import no.nav.veilarbvedtaksstotte.service.BigQueryService;
import no.nav.veilarbvedtaksstotte.service.DistribusjonService;
import no.nav.veilarbvedtaksstotte.service.DokumentService;
import no.nav.veilarbvedtaksstotte.service.DvhRapporteringService;
import no.nav.veilarbvedtaksstotte.service.EnhetInfoService;
import no.nav.veilarbvedtaksstotte.service.Gjeldende14aVedtakService;
import no.nav.veilarbvedtaksstotte.service.KafkaConsumerService;
import no.nav.veilarbvedtaksstotte.service.KafkaProducerService;
import no.nav.veilarbvedtaksstotte.service.KafkaRepubliseringService;
import no.nav.veilarbvedtaksstotte.service.KafkaVedtakStatusEndringConsumer;
import no.nav.veilarbvedtaksstotte.service.MalTypeService;
import no.nav.veilarbvedtaksstotte.service.MeldingService;
import no.nav.veilarbvedtaksstotte.service.MetricsService;
import no.nav.veilarbvedtaksstotte.service.OyeblikksbildeService;
import no.nav.veilarbvedtaksstotte.service.PdfService;
import no.nav.veilarbvedtaksstotte.service.SakStatistikkResendingService;
import no.nav.veilarbvedtaksstotte.service.SakStatistikkService;
import no.nav.veilarbvedtaksstotte.service.Siste14aVedtakService;
import no.nav.veilarbvedtaksstotte.service.VedtakHendelserService;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import no.nav.veilarbvedtaksstotte.service.VeilarbarenaService;
import no.nav.veilarbvedtaksstotte.service.VeilederService;
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
        PdfService.class,
        Gjeldende14aVedtakService.class,
        AuditlogService.class
})
public class ServiceTestConfig {
}
