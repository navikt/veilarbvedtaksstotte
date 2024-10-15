package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.controller.AdminController;
import no.nav.veilarbvedtaksstotte.controller.BeslutterController;
import no.nav.veilarbvedtaksstotte.controller.BeslutteroversiktController;
import no.nav.veilarbvedtaksstotte.controller.Frontendlogger;
import no.nav.veilarbvedtaksstotte.controller.InternalController;
import no.nav.veilarbvedtaksstotte.controller.KodeverkController;
import no.nav.veilarbvedtaksstotte.controller.MeldingController;
import no.nav.veilarbvedtaksstotte.controller.Siste14aVedtakController;
import no.nav.veilarbvedtaksstotte.controller.UtkastController;
import no.nav.veilarbvedtaksstotte.controller.UtrullingController;
import no.nav.veilarbvedtaksstotte.controller.VedtakController;
import no.nav.veilarbvedtaksstotte.controller.v2.Siste14aVedtakV2Controller;
import no.nav.veilarbvedtaksstotte.controller.v2.UtkastV2Controller;
import no.nav.veilarbvedtaksstotte.controller.v2.UtrullingV2Controller;
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
        UtrullingController.class,
        AdminController.class,
        Siste14aVedtakController.class,
        Frontendlogger.class,
        KodeverkController.class,
        Siste14aVedtakV2Controller.class,
        UtkastV2Controller.class,
        UtrullingV2Controller.class,
        VedtakV2Controller.class
})
public class ControllerTestConfig {}
