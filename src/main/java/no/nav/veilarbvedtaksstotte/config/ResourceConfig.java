package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.resource.VedtakResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import no.nav.veilarbvedtaksstotte.resource.BeslutterResource;

@Configuration
@Import({ VedtakResource.class, BeslutterResource.class })
public class ResourceConfig {}
