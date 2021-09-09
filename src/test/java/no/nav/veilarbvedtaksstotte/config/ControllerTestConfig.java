package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.controller.*;
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
        Siste14aVedtakController.class
})
public class ControllerTestConfig {}
