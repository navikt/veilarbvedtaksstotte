package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.resource.VedtakResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import no.nav.fo.veilarbvedtaksstotte.resource.BeslutterResource;

@Slf4j
@Configuration
@Import({ VedtakResource.class, BeslutterResource.class })
public class ResourceConfig {}
