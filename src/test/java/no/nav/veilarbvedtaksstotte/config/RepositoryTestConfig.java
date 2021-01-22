package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.repository.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        BeslutteroversiktRepository.class,
        KafkaRepository.class,
        KilderRepository.class,
        MeldingRepository.class,
        OyeblikksbildeRepository.class,
        VedtaksstotteRepository.class,
        ArenaVedtakRepository.class
})
public class RepositoryTestConfig {}
