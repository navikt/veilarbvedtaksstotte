package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.service.MalTypeService;
import no.nav.fo.veilarbvedtaksstotte.service.VedtakService;
import no.nav.fo.veilarbvedtaksstotte.service.VeilederService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import({ VeilederService.class, MalTypeService.class, VedtakService.class })
public class ServiceConfig {}
