package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.repository.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        BeslutteroversiktRepository.class,
        KilderRepository.class,
        MeldingRepository.class,
        OyeblikksbildeRepository.class,
        VedtaksstotteRepository.class,
        UtrullingRepository.class,
        ArenaVedtakRepository.class
})
public class RepositoryTestConfig {}
