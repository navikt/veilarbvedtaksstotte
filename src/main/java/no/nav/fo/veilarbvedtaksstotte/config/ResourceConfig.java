package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.fo.veilarbvedtaksstotte.resource.VedtakResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import no.nav.fo.veilarbvedtaksstotte.resource.BeslutterResource;

@Configuration
@Import({ VedtakResource.class, BeslutterResource.class })
public class ResourceConfig {}
