package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaAvsluttOppfolging;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak;
import no.nav.veilarbvedtaksstotte.domain.kafka.ArenaVedtakRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
@Slf4j
public class KafkaConsumerService {
    private final VedtakService vedtakService;
    private final InnsatsbehovService innsatsbehovService;

    @Autowired
    public KafkaConsumerService(
            @Lazy VedtakService vedtakService,
            @Lazy InnsatsbehovService innsatsbehovService
    ) {
        this.vedtakService = vedtakService;
        this.innsatsbehovService = innsatsbehovService;
    }

    public void behandleEndringPaAvsluttOppfolging(KafkaAvsluttOppfolging kafkaAvsluttOppfolging) {
        vedtakService.behandleAvsluttOppfolging(kafkaAvsluttOppfolging);
    }

    public void behandleEndringPaOppfolgingsbruker(KafkaOppfolgingsbrukerEndring kafkaOppfolgingsbrukerEndring) {
        vedtakService.behandleOppfolgingsbrukerEndring(kafkaOppfolgingsbrukerEndring);
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
