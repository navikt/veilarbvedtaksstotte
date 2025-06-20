package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.controller.*;
import no.nav.veilarbvedtaksstotte.controller.v2.Siste14aVedtakV2Controller;
import no.nav.veilarbvedtaksstotte.controller.v2.UtkastV2Controller;
import no.nav.veilarbvedtaksstotte.controller.v2.VedtakV2Controller;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        BeslutterController.class,
        BeslutteroversiktController.class,
        InternalController.class,
        MeldingController.class,
        VedtakController.class,
        UtkastController.class,
        AdminController.class,
        Siste14aVedtakController.class,
        Frontendlogger.class,
        KodeverkController.class,
        Siste14aVedtakV2Controller.class,
        UtkastV2Controller.class,
        VedtakV2Controller.class,
        Gjeldende14aVedtakController.class
})
public class ControllerTestConfig {}
