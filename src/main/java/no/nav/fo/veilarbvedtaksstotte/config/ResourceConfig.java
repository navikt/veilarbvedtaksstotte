package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.client.DokumentClient;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.resource.VedtakResource;
import no.nav.fo.veilarbvedtaksstotte.service.VeilederService;
import org.springframework.context.annotation.Bean;

@Slf4j
public class ResourceConfig {

    @Bean
    public VedtakResource vedtakResource(VedtaksstotteRepository vedtaksstotteRepository,
                                         PepClient pepClient,
                                         AktorService aktorService,
                                         VeilederService veilederService,
                                         DokumentClient dokumentClient) {
        return new VedtakResource(vedtaksstotteRepository, pepClient,
                aktorService, veilederService, dokumentClient);
    }

}
