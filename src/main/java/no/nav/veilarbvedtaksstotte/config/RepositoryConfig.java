package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.repository.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        VedtaksstotteRepository.class,
        KafkaRepository.class,
        OyeblikksbildeRepository.class,
        KilderRepository.class,
        DialogRepository.class
})
public class RepositoryConfig {}
