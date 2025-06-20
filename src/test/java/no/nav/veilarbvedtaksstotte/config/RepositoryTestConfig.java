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
        ArenaVedtakRepository.class,
        SakStatistikkRepository.class,
        SisteOppfolgingPeriodeRepository.class
})
public class RepositoryTestConfig {}
