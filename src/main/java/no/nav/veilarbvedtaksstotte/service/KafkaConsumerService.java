package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.client.utils.graphql.GraphqlErrorException;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.IdUtils;
import no.nav.person.pdl.aktor.v2.Aktor;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndringV2;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiode;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiodeV3;
import no.nav.veilarbvedtaksstotte.domain.kafka.SisteEndringsType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog;

@Service
@Slf4j
public class KafkaConsumerService {

    private final VedtaksstotteRepository vedtaksstotteRepository;

    private final BeslutteroversiktRepository beslutteroversiktRepository;

    private final Norg2Client norg2Client;

    private final AktorOppslagClient aktorOppslagClient;

    private final VeilarbarenaClient veilarbarenaClient;

    private final SisteOppfolgingPeriodeRepository sisteOppfolgingPeriodeRepository;

    private final BrukerIdenterService brukerIdenterService;

    private final KafkaProducerService kafkaProducerService;

    private static final String MDC_KAFKA_CONSUMER_SERVICE_CORRELATION_ID_KEY = "kafka_consumer_correlation_id";

    @Autowired
    public KafkaConsumerService(
            VedtaksstotteRepository vedtaksstotteRepository,
            BeslutteroversiktRepository beslutteroversiktRepository,
            SisteOppfolgingPeriodeRepository sisteOppfolgingPeriodeRepository,
            Norg2Client norg2Client,
            AktorOppslagClient aktorOppslagClient,
            VeilarbarenaClient veilarbarenaClient,
            BrukerIdenterService brukerIdenterService,
            KafkaProducerService kafkaProducerService
    ) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.sisteOppfolgingPeriodeRepository = sisteOppfolgingPeriodeRepository;
        this.norg2Client = norg2Client;
        this.aktorOppslagClient = aktorOppslagClient;
        this.veilarbarenaClient = veilarbarenaClient;
        this.brukerIdenterService = brukerIdenterService;
        this.kafkaProducerService = kafkaProducerService;
    }

    public <K, V> void behandleKafkaMelding(ConsumerRecord<K, V> melding, KafkaMeldingBehandler<K, V> meldingBehandler) {
        MDC.put(MDC_KAFKA_CONSUMER_SERVICE_CORRELATION_ID_KEY, IdUtils.generateId());
        try {
            log.info("Behandler Kafka-melding. Topic: {}, offset: {}, partisjon: {}.", melding.topic(), melding.offset(), melding.partition());
            meldingBehandler.behandleMelding(melding);
        } finally {
            MDC.remove(MDC_KAFKA_CONSUMER_SERVICE_CORRELATION_ID_KEY);
        }
    }

    public void behandleOppfolgingsPeriodeStartet(KafkaSisteOppfolgingsperiodeV3 melding) {
        sisteOppfolgingPeriodeRepository.upsertSisteOppfolgingPeriode(melding.getOppfolgingsperiodeUuid(), melding.getAktorId(), melding.getStartTidspunkt(), null);
        log.info("Siste oppfølgingsperiode har blitt upsertet");

    }

    public void behandleOppfolgingsPeriodeAvsluttet(KafkaSisteOppfolgingsperiodeV3 melding) {
        ZonedDateTime sluttTidspunkt = melding.getSluttTidspunkt();

        sisteOppfolgingPeriodeRepository.upsertSisteOppfolgingPeriode(melding.getOppfolgingsperiodeUuid(), melding.getAktorId(), melding.getStartTidspunkt(), melding.getSluttTidspunkt());
        log.info("Siste oppfølgingsperiode har blitt upsertet");

        Vedtak gjeldendeVedtak = vedtaksstotteRepository.hentGjeldendeVedtak(melding.getAktorId());

        if (gjeldendeVedtak == null) {
            log.info("Brukeren har ingen gjeldende vedtak - ignorerer melding.");
            return;
        }

        LocalDateTime vedtakFattetDato = gjeldendeVedtak.getVedtakFattet();
        boolean vedtakFattetForOppfolgingAvsluttet = vedtakFattetDato.isBefore(sluttTidspunkt.toLocalDateTime());

        if (!vedtakFattetForOppfolgingAvsluttet) {
            log.warn("Gjeldende vedtak {} har startdato etter at siste oppfølgingsperiode {} ble " +
                    "avsluttet. Vi kan derfor ikke sette vedtak til historisk. Man bør verifisere om brukeren er under " +
                    "oppfølging eller ikke og eventuelt korrigere vedtaket manuelt.", gjeldendeVedtak.getId(), melding.getOppfolgingsperiodeUuid());
            return;
        }

        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(gjeldendeVedtak.getId());
        log.info("Gjeldende vedtak {} satt til historisk", gjeldendeVedtak.getId());

        kafkaProducerService.sendGjeldende14aVedtak(new AktorId(gjeldendeVedtak.getAktorId()), null);
    }

    public void flyttingAvBrukerTilNyEnhet(KafkaSisteOppfolgingsperiodeV3 melding) {
        String kontorId = melding.getKontor().getKontorId();

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(melding.getAktorId());

        if (utkast == null) {
            log.info("Fant ingen utkast for bruker, ignorerer melding.");
            return;
        }

        if (utkast.getOppfolgingsenhetId().equals(kontorId)) {
            log.info("Oppfølgingsenhet for bruker er uendret, ignorerer melding.");
            return;
        }

        log.info("Oppfølgingsenhet for bruker er endret, flytter utkast til ny enhet. Se SecureLogs for detaljer.");
        secureLog.info("Oppfølgingsenhet for bruker er endret, flytter utkast til ny enhet. Bruker (AktørID): {}, forrige oppfølgingsenhet: {}, ny oppfølgingsenhet: {}.", melding.getAktorId(), utkast.getOppfolgingsenhetId(), kontorId);
        vedtaksstotteRepository.oppdaterUtkastEnhet(utkast.getId(), kontorId);
        beslutteroversiktRepository.oppdaterBrukerEnhet(utkast.getId(), kontorId, melding.getKontor().getKontorNavn());
    }

    public void behandleSisteOppfolgingsperiodeV3(ConsumerRecord<Long, KafkaSisteOppfolgingsperiodeV3> sisteOppfolgingsperiodeRecord) {
        switch (sisteOppfolgingsperiodeRecord.value().getSisteEndringsType()) {
            case ARBEIDSOPPFOLGINGSKONTOR_ENDRET ->
                    flyttingAvBrukerTilNyEnhet(sisteOppfolgingsperiodeRecord.value());
            case OPPFOLGING_STARTET ->
                    behandleOppfolgingsPeriodeStartet(sisteOppfolgingsperiodeRecord.value());
            case OPPFOLGING_AVSLUTTET ->
                    behandleOppfolgingsPeriodeAvsluttet(sisteOppfolgingsperiodeRecord.value());
            default -> {
                // Ingen handling
            }
        }
    }

    public void behandlePdlAktorV2Melding(ConsumerRecord<String, Aktor> aktorRecord) {
        brukerIdenterService.behandlePdlAktorV2Melding(aktorRecord);
    }

    private AktorId hentAktorIdMedDevSjekk(Fnr fnr) {
        try {
            return aktorOppslagClient.hentAktorId(fnr);
        } catch (GraphqlErrorException | IngenGjeldendeIdentException e) {
            if (isDevelopment().orElse(false)) {
                log.info("Prøvde å hente prodlik bruker i dev. Returnerer null");
                return null;
            } else throw e;
        }
    }
}