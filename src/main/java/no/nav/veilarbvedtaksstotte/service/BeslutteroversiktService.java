package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service
public class BeslutteroversiktService {

    private final BeslutteroversiktRepository beslutteroversiktRepository;

    @Inject
    public BeslutteroversiktService(BeslutteroversiktRepository beslutteroversiktRepository) {
        this.beslutteroversiktRepository = beslutteroversiktRepository;
    }

    public List<BeslutteroversiktBruker> sokEtterBruker(BeslutteroversiktSok sok) {
        return beslutteroversiktRepository.sokEtterBrukere(sok);
    }

}
