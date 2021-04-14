package no.nav.veilarbvedtaksstotte.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KafkaConsumerService {

    private final VedtaksstotteRepository vedtaksstotteRepository;

    private final BeslutteroversiktRepository beslutteroversiktRepository;

    private final Norg2Client norg2Client;

    public void behandleEndringPaAvsluttOppfolging(KafkaAvsluttOppfolging kafkaAvsluttOppfolging) {
        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(kafkaAvsluttOppfolging.getAktorId());
    }

    public void behandleEndringPaOppfolgingsbruker(KafkaOppfolgingsbrukerEndring kafkaOppfolgingsbrukerEndring) {
        String aktorId = kafkaOppfolgingsbrukerEndring.getAktorId();
        String oppfolgingsenhetId = kafkaOppfolgingsbrukerEndring.getOppfolgingsenhetId();

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast != null && !utkast.getOppfolgingsenhetId().equals(oppfolgingsenhetId)) {
            Enhet enhet = norg2Client.hentEnhet(oppfolgingsenhetId);
            vedtaksstotteRepository.oppdaterUtkastEnhet(utkast.getId(), oppfolgingsenhetId);
            beslutteroversiktRepository.oppdaterBrukerEnhet(utkast.getId(), oppfolgingsenhetId, enhet.getNavn());
        }
    }

}
