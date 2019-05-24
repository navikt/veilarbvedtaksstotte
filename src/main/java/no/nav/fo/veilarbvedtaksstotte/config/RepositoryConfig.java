package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.OpplysningerRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.OyblikksbildeRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import({
        VedtaksstotteRepository.class,
        KafkaRepository.class,
        OyblikksbildeRepository.class,
        OpplysningerRepository.class
})
public class RepositoryConfig {}
