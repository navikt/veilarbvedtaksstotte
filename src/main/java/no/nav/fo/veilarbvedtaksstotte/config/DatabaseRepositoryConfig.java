package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.fo.veilarbvedtaksstotte.db.VedtaksstotteRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        VedtaksstotteRepository.class
})
public class DatabaseRepositoryConfig {}

