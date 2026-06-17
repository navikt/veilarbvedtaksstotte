package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaSisteOppfolgingsperiodeV3
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository
import no.nav.veilarbvedtaksstotte.repository.SisteOppfolgingPeriodeRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KafkaSisteOppfolgingsperiodeV3Consumer(
    private val vedtaksstotteRepository: VedtaksstotteRepository,
    private val beslutteroversiktRepository: BeslutteroversiktRepository,
    private val sisteOppfolgingPeriodeRepository: SisteOppfolgingPeriodeRepository,
    private val kafkaProducerService: KafkaProducerService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun behandleSisteOppfolgingsperiodeV3(record: ConsumerRecord<Long, KafkaSisteOppfolgingsperiodeV3>) {
        when (record.value().sisteEndringsType) {
            no.nav.veilarbvedtaksstotte.domain.kafka.SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET ->
                flyttingAvBrukerTilNyEnhet(record)
            no.nav.veilarbvedtaksstotte.domain.kafka.SisteEndringsType.OPPFOLGING_STARTET ->
                behandleOppfolgingsPeriodeStartet(record)
            no.nav.veilarbvedtaksstotte.domain.kafka.SisteEndringsType.OPPFOLGING_AVSLUTTET ->
                behandleOppfolgingsPeriodeAvsluttet(record)
        }
    }

    private fun flyttingAvBrukerTilNyEnhet(kafkaRecord: ConsumerRecord<Long, KafkaSisteOppfolgingsperiodeV3>) {
        val melding = kafkaRecord.value()
        if (melding.kontor == null) {
            log.warn("Mottok ARBEIDSOPPFOLGINGSKONTOR_ENDRET-melding uten kontor. Topic: {}, partisjon: {}, offset: {} - ignorerer melding.",
                kafkaRecord.topic(), kafkaRecord.partition(), kafkaRecord.offset())
            return
        }

        val kontorId = melding.kontor.kontorId

        val utkast = vedtaksstotteRepository.hentUtkast(melding.aktorId)

        if (utkast == null) {
            log.info("Fant ingen utkast for bruker, ignorerer melding.")
            return
        }

        if (utkast.oppfolgingsenhetId == kontorId) {
            log.info("Oppfølgingsenhet for bruker er uendret, ignorerer melding.")
            return
        }

        val lagretPeriode = sisteOppfolgingPeriodeRepository.hentSisteOppfolgingsperiode(AktorId.of(melding.aktorId))

        if (lagretPeriode != null && melding.startTidspunkt.isBefore(lagretPeriode.startdato)) {
            log.info("Mottok utdatert ARBEIDSOPPFOLGINGSKONTOR_ENDRET-melding (startTidspunkt {} er før lagret startdato {}). Topic: {}, partisjon: {}, offset: {} - ignorerer.",
                melding.startTidspunkt, lagretPeriode.startdato, kafkaRecord.topic(), kafkaRecord.partition(), kafkaRecord.offset())
            return
        }

        log.info("Oppfølgingsenhet for bruker er endret, flytter utkast til ny enhet. Se SecureLogs for detaljer.")
        secureLog.info("Oppfølgingsenhet for bruker er endret, flytter utkast til ny enhet. Bruker (AktørID): {}, forrige oppfølgingsenhet: {}, ny oppfølgingsenhet: {}.",
            melding.aktorId, utkast.oppfolgingsenhetId, kontorId)
        vedtaksstotteRepository.oppdaterUtkastEnhet(utkast.id, kontorId)
        beslutteroversiktRepository.oppdaterBrukerEnhet(utkast.id, kontorId, melding.kontor.kontorNavn)
    }

    private fun behandleOppfolgingsPeriodeStartet(kafkaRecord: ConsumerRecord<Long, KafkaSisteOppfolgingsperiodeV3>) {
        val melding = kafkaRecord.value()
        val lagretPeriode = sisteOppfolgingPeriodeRepository.hentSisteOppfolgingsperiode(AktorId.of(melding.aktorId))

        if (lagretPeriode != null && melding.startTidspunkt.isBefore(lagretPeriode.startdato)) {
            log.info("Mottok utdatert OPPFOLGING_STARTET-melding (startTidspunkt {} er før lagret startdato {}). Topic: {}, partisjon: {}, offset: {} - ignorerer.",
                melding.startTidspunkt, lagretPeriode.startdato, kafkaRecord.topic(), kafkaRecord.partition(), kafkaRecord.offset())
            return
        }

        sisteOppfolgingPeriodeRepository.upsertSisteOppfolgingPeriode(melding.oppfolgingsperiodeUuid, melding.aktorId, melding.startTidspunkt, null)
        log.info("Siste oppfølgingsperiode har blitt upsertet")
    }

    private fun behandleOppfolgingsPeriodeAvsluttet(kafkaRecord: ConsumerRecord<Long, KafkaSisteOppfolgingsperiodeV3>) {
        val melding = kafkaRecord.value()
        val sluttTidspunkt = melding.sluttTidspunkt

        if (sluttTidspunkt == null) {
            log.warn("Mottok OPPFOLGING_AVSLUTTET-melding uten sluttTidspunkt - ignorerer melding.")
            return
        }

        val lagretPeriode = sisteOppfolgingPeriodeRepository.hentSisteOppfolgingsperiode(AktorId.of(melding.aktorId))

        if (lagretPeriode != null && sluttTidspunkt.isBefore(lagretPeriode.startdato)) {
            log.info("Mottok utdatert OPPFOLGING_AVSLUTTET-melding (sluttTidspunkt {} er før lagret startdato {}). Topic: {}, partisjon: {}, offset: {} - ignorerer.",
                sluttTidspunkt, lagretPeriode.startdato, kafkaRecord.topic(), kafkaRecord.partition(), kafkaRecord.offset())
            return
        }

        sisteOppfolgingPeriodeRepository.upsertSisteOppfolgingPeriode(melding.oppfolgingsperiodeUuid, melding.aktorId, melding.startTidspunkt, sluttTidspunkt)
        log.info("Siste oppfølgingsperiode har blitt upsertet")

        val gjeldendeVedtak = vedtaksstotteRepository.hentGjeldendeVedtak(melding.aktorId)

        if (gjeldendeVedtak == null) {
            log.info("Brukeren har ingen gjeldende vedtak - ignorerer melding.")
            return
        }

        val vedtakFattetDato = gjeldendeVedtak.vedtakFattet
        val vedtakFattetForOppfolgingAvsluttet = vedtakFattetDato.isBefore(sluttTidspunkt.toLocalDateTime())

        if (!vedtakFattetForOppfolgingAvsluttet) {
            log.warn("Gjeldende vedtak {} har startdato etter at siste oppfølgingsperiode {} ble " +
                "avsluttet. Vi kan derfor ikke sette vedtak til historisk. Man bør verifisere om brukeren er under " +
                "oppfølging eller ikke og eventuelt korrigere vedtaket manuelt.", gjeldendeVedtak.id, melding.oppfolgingsperiodeUuid)
            return
        }

        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(gjeldendeVedtak.id)
        log.info("Gjeldende vedtak {} satt til historisk", gjeldendeVedtak.id)

        kafkaProducerService.sendGjeldende14aVedtak(AktorId(gjeldendeVedtak.aktorId), null)
    }
}
