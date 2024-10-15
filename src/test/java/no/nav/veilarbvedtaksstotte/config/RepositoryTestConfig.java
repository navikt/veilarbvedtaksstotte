package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.veilarbvedtaksstotte.repository.UtrullingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
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
