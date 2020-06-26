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
        UtkastController.class
})
public class ControllerTestConfig {}
