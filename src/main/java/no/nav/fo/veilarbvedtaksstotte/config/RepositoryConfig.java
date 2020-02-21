package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        VedtaksstotteRepository.class,
        KafkaRepository.class,
        OyeblikksbildeRepository.class,
        KilderRepository.class
})
public class RepositoryConfig {}
