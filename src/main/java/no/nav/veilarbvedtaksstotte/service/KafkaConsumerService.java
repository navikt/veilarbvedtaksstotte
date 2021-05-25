package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.domain.kafka.ArenaVedtakRecord;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
@Slf4j
public class KafkaConsumerService {

    private final InnsatsbehovService innsatsbehovService;

    private final KafkaProducerService kafkaProducerService;

    private final VedtaksstotteRepository vedtaksstotteRepository;

    private final BeslutteroversiktRepository beslutteroversiktRepository;

    private final Norg2Client norg2Client;

    @Autowired
    public KafkaConsumerService(
            @Lazy InnsatsbehovService innsatsbehovService,
            @Lazy KafkaProducerService kafkaProducerService,
            VedtaksstotteRepository vedtaksstotteRepository,
            BeslutteroversiktRepository beslutteroversiktRepository,
            Norg2Client norg2Client
    ) {
        this.innsatsbehovService = innsatsbehovService;
        this.kafkaProducerService =kafkaProducerService;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.norg2Client = norg2Client;
    }

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

    public void behandleArenaVedtak(ArenaVedtakRecord arenaVedtakRecord) {
        ArenaVedtak arenaVedtak = ArenaVedtak.fraRecord(arenaVedtakRecord);
        if (arenaVedtak != null) {
            innsatsbehovService.behandleEndringFraArena(arenaVedtak);
        } else {
            log.info(format("Behandler ikke melding fra Arena med kvalifiseringsgruppe = %s og hovedmål = %s",
                    arenaVedtakRecord.getAfter().getKvalifiseringsgruppe(),
                    arenaVedtakRecord.getAfter().getHovedmal()));
        }
    }
}
