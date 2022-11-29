package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.domain.kafka.ArenaVedtakRecord;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndringV2;
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
@Slf4j
public class KafkaConsumerService {

    private final Siste14aVedtakService siste14aVedtakService;

    private final VedtaksstotteRepository vedtaksstotteRepository;

    private final BeslutteroversiktRepository beslutteroversiktRepository;

    private final Norg2Client norg2Client;

    private final AktorOppslagClient aktorOppslagClient;
    @Autowired
    public KafkaConsumerService(
            Siste14aVedtakService siste14aVedtakService,
            VedtaksstotteRepository vedtaksstotteRepository,
            BeslutteroversiktRepository beslutteroversiktRepository,
            Norg2Client norg2Client,
            AktorOppslagClient aktorOppslagClient
    ) {
        this.siste14aVedtakService = siste14aVedtakService;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.norg2Client = norg2Client;
        this.aktorOppslagClient = aktorOppslagClient;
    }

    public void behandleEndringPaAvsluttOppfolging(ConsumerRecord<String, KafkaAvsluttOppfolging> kafkaAvsluttOppfolging) {
        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(kafkaAvsluttOppfolging.value().getAktorId());
    }

    public void behandleEndringPaOppfolgingsbrukerOnprem(ConsumerRecord<String, KafkaOppfolgingsbrukerEndring> kafkaOppfolgingsbrukerEndring) {
        String aktorId = kafkaOppfolgingsbrukerEndring.value().getAktorId();
        String oppfolgingsenhetId = kafkaOppfolgingsbrukerEndring.value().getOppfolgingsenhetId();

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast != null && !utkast.getOppfolgingsenhetId().equals(oppfolgingsenhetId)) {
            Enhet enhet = norg2Client.hentEnhet(oppfolgingsenhetId);
            vedtaksstotteRepository.oppdaterUtkastEnhet(utkast.getId(), oppfolgingsenhetId);
            beslutteroversiktRepository.oppdaterBrukerEnhet(utkast.getId(), oppfolgingsenhetId, enhet.getNavn());
        }
    }

    public void behandleEndringPaOppfolgingsbruker(ConsumerRecord<String, KafkaOppfolgingsbrukerEndringV2> kafkaOppfolgingsbrukerEndring) {
        String fnr = kafkaOppfolgingsbrukerEndring.value().getFodselsnummer();
        AktorId aktorId = aktorOppslagClient.hentAktorId(Fnr.ofValidFnr(fnr));
        String oppfolgingsenhetId = kafkaOppfolgingsbrukerEndring.value().getOppfolgingsenhet();

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId.toString());

        if (utkast != null && !utkast.getOppfolgingsenhetId().equals(oppfolgingsenhetId)) {
            Enhet enhet = norg2Client.hentEnhet(oppfolgingsenhetId);
            vedtaksstotteRepository.oppdaterUtkastEnhet(utkast.getId(), oppfolgingsenhetId);
            beslutteroversiktRepository.oppdaterBrukerEnhet(utkast.getId(), oppfolgingsenhetId, enhet.getNavn());
        }
    }

    public void behandleArenaVedtak(ConsumerRecord<String, ArenaVedtakRecord> arenaVedtakRecord) {
        ArenaVedtak arenaVedtak = ArenaVedtak.fraRecord(arenaVedtakRecord.value());
        if (arenaVedtak != null) {
            siste14aVedtakService.behandleEndringFraArena(arenaVedtak);
        } else {
            log.info(format("Behandler ikke melding fra Arena med kvalifiseringsgruppe = %s og hovedmål = %s",
                    arenaVedtakRecord.value().getAfter().getKvalifiseringsgruppe(),
                    arenaVedtakRecord.value().getAfter().getHovedmal()));
        }
    }
}
