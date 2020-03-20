package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.resource.BeslutterResource;
import no.nav.veilarbvedtaksstotte.resource.DialogResource;
import no.nav.veilarbvedtaksstotte.resource.VedtakResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ VedtakResource.class, BeslutterResource.class, DialogResource.class })
public class ResourceConfig {}
