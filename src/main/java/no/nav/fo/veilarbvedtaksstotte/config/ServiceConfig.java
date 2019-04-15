package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.client.DokumentClient;
import no.nav.fo.veilarbvedtaksstotte.client.PersonClient;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.service.MalTypeService;
import no.nav.fo.veilarbvedtaksstotte.service.VedtakService;
import no.nav.fo.veilarbvedtaksstotte.service.VeilederService;
import org.springframework.context.annotation.Bean;

@Slf4j
public class ServiceConfig {

    @Bean
    public VeilederService veilederService() {
        return new VeilederService();
    }

    @Bean
    public MalTypeService malTypeService() {
        return new MalTypeService();
    }

    @Bean
    public VedtakService vedtakService(VedtaksstotteRepository vedtaksstotteRepository,
                                       PepClient pepClient,
                                       AktorService aktorService,
                                       DokumentClient dokumentClient,
                                       PersonClient personClient,
                                       VeilederService veilederService,
                                       MalTypeService malTypeService) {
        return new VedtakService(vedtaksstotteRepository, pepClient,
                aktorService, dokumentClient, personClient, veilederService,
                malTypeService);
    }

}
