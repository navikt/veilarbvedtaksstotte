package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.client.utils.graphql.GraphqlErrorException;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.person.pdl.aktor.v2.Aktor;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.domain.kafka.ArenaVedtakRecord;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaOppfolgingsbrukerEndringV2;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiode;
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.SecureLog;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static java.lang.String.format;
import static no.nav.common.utils.EnvironmentUtils.isDevelopment;

@Service
@Slf4j
public class KafkaConsumerService {

    private final Siste14aVedtakService siste14aVedtakService;

    private final VedtaksstotteRepository vedtaksstotteRepository;

    private final BeslutteroversiktRepository beslutteroversiktRepository;

    private final Norg2Client norg2Client;

    private final AktorOppslagClient aktorOppslagClient;

    private final VeilarbarenaClient veilarbarenaClient;

    private final SisteOppfolgingPeriodeRepository sisteOppfolgingPeriodeRepository;

    private final BrukerIdenterService brukerIdenterService;

    @Autowired
    public KafkaConsumerService(
            Siste14aVedtakService siste14aVedtakService,
            VedtaksstotteRepository vedtaksstotteRepository,
            BeslutteroversiktRepository beslutteroversiktRepository,
            SisteOppfolgingPeriodeRepository sisteOppfolgingPeriodeRepository,
            Norg2Client norg2Client,
            AktorOppslagClient aktorOppslagClient,
            VeilarbarenaClient veilarbarenaClient,
            BrukerIdenterService brukerIdenterService
    ) {
        this.siste14aVedtakService = siste14aVedtakService;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.sisteOppfolgingPeriodeRepository = sisteOppfolgingPeriodeRepository;
        this.norg2Client = norg2Client;
        this.aktorOppslagClient = aktorOppslagClient;
        this.veilarbarenaClient = veilarbarenaClient;
        this.brukerIdenterService = brukerIdenterService;
    }

    public void flyttingAvOppfolgingsbrukerTilNyEnhet(ConsumerRecord<String, KafkaOppfolgingsbrukerEndringV2> kafkaOppfolgingsbrukerEndring) {
        log.info("Behandler melding på topic {}.", kafkaOppfolgingsbrukerEndring.topic());

        Fnr fnr = kafkaOppfolgingsbrukerEndring.value().getFodselsnummer();

        veilarbarenaClient.oppdaterOppfolgingsbruker(fnr, kafkaOppfolgingsbrukerEndring.value().getOppfolgingsenhet());

        AktorId aktorId = hentAktorIdMedDevSjekk(fnr); //AktorId kan være null i dev
        String oppfolgingsenhetId = kafkaOppfolgingsbrukerEndring.value().getOppfolgingsenhet();

        if (aktorId == null) {
            log.warn("Fant ingen AktørID for bruker. Ignorerer melding. Se SecureLogs for detaljer.");
            SecureLog.getSecureLog().warn("Fant ingen AktørID for bruker. Ignorerer melding. Bruker (fnr): {}", fnr);
            return;
        }

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId.toString());

        if (utkast == null) {
            log.info("Fant ingen utkast for bruker, ignorerer melding.");
            return;
        }

        if (utkast.getOppfolgingsenhetId().equals(oppfolgingsenhetId)) {
            log.info("Oppfølgingsenhet for bruker er uendret, ignorerer melding.");
            return;
        }

        log.info("Oppfølgingsenhet for bruker er endret, flytter utkast til ny enhet. Se SecureLogs for detaljer.");
        SecureLog.getSecureLog().info("Oppfølgingsenhet for bruker er endret, flytter utkast til ny enhet. Bruker (AktørID): {}, forrige oppfølgingsenhet: {}, ny oppfølgingsenhet: {}.", aktorId, utkast.getOppfolgingsenhetId(), oppfolgingsenhetId);
        Enhet enhet = norg2Client.hentEnhet(oppfolgingsenhetId);
        vedtaksstotteRepository.oppdaterUtkastEnhet(utkast.getId(), oppfolgingsenhetId);
        beslutteroversiktRepository.oppdaterBrukerEnhet(utkast.getId(), oppfolgingsenhetId, enhet.getNavn());
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

    public void behandleSisteOppfolgingsperiode(ConsumerRecord<String, KafkaSisteOppfolgingsperiode> sisteOppfolgingsperiodeRecord) {
        KafkaSisteOppfolgingsperiode sisteOppfolgingsperiode = sisteOppfolgingsperiodeRecord.value();

        if (sisteOppfolgingsperiode == null) {
            log.warn("Record for topic {} inneholdt tom verdi - ignorerer melding.", sisteOppfolgingsperiodeRecord.topic());
            return;
        }

        ZonedDateTime startDato = sisteOppfolgingsperiode.getStartDato();
        ZonedDateTime sluttDato = sisteOppfolgingsperiode.getSluttDato();

        if (startDato == null && sluttDato != null) {
            throw new IllegalStateException("Oppfølgingsperiode har sluttdato men ingen startdato.");
        }

        if (startDato != null) {
            sisteOppfolgingPeriodeRepository.upsertSisteOppfolgingPeriode(sisteOppfolgingsperiode);
            log.info("Siste oppfølgingsperiode har blitt upsertet");
        }

        if (sluttDato == null) {
            // Vi er bare interessert i oppfølgingsperiode dersom den er avsluttet, dvs. sluttDato != null
            log.debug("Siste oppfølgingsperiode har ingen sluttdato - ignorerer melding.");
            return;
        }

        String aktorId = sisteOppfolgingsperiode.getAktorId();
        Vedtak gjeldendeVedtak = vedtaksstotteRepository.hentGjeldendeVedtak(aktorId);

        if (gjeldendeVedtak == null) {
            log.debug("Brukeren har ingen gjeldende vedtak - ignorerer melding.");
            return;
        }

        LocalDateTime vedtakFattetDato = gjeldendeVedtak.getVedtakFattet();
        boolean vedtakFattetForOppfolgingAvsluttet = vedtakFattetDato.isBefore(sluttDato.toLocalDateTime());

        if (!vedtakFattetForOppfolgingAvsluttet) {
            log.warn("Gjeldende vedtak {} har startdato etter at siste oppfølgingsperiode {} ble " +
                    "avsluttet. Vi kan derfor ikke sette vedtak til historisk. Man bør verifisere om brukeren er under " +
                    "oppfølging eller ikke og eventuelt korrigere vedtaket manuelt.", gjeldendeVedtak.getId(), sisteOppfolgingsperiode.getUuid());
            return;
        }

        log.info("Setter gjeldende vedtak {} til historisk", gjeldendeVedtak.getId());
        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(gjeldendeVedtak.getId());
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
