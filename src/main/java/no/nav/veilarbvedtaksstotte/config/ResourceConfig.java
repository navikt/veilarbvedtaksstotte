package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.resource.BeslutterResource;
import no.nav.veilarbvedtaksstotte.resource.BeslutteroversiktResource;
import no.nav.veilarbvedtaksstotte.resource.MeldingResource;
import no.nav.veilarbvedtaksstotte.resource.VedtakResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ VedtakResource.class, BeslutterResource.class, MeldingResource.class, BeslutteroversiktResource.class })
public class ResourceConfig {}
