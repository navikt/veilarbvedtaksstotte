package no.nav.veilarbvedtaksstotte.service

import io.getunleash.DefaultUnleash
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.config.EnvironmentProperties
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingMetode
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingStatus
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingType
import no.nav.veilarbvedtaksstotte.domain.statistikk.Fagsystem
import no.nav.veilarbvedtaksstotte.domain.statistikk.SAK_YTELSE
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import no.nav.veilarbvedtaksstotte.domain.statistikk.toBehandlingResultat
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.SAK_STATISTIKK_PAA
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset
import java.util.*


@Service
class SakStatistikkService @Autowired constructor(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val bigQueryService: BigQueryService,
    private val unleashClient: DefaultUnleash,
    private val environmentProperties: EnvironmentProperties,
    private val vedtaksstotteRepository: VedtaksstotteRepository,
    private val siste14aVedtakService: Siste14aVedtakService

) {
    fun lagreSakstatistikkrad(vedtak: Vedtak, fnr: Fnr) {
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        if (statistikkPaa) {
            val oppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
            val eksisterendeVedtak = vedtaksstotteRepository.hentFattedeVedtak(vedtak.aktorId)
            val siste14aVedtakArena = siste14aVedtakService.siste14aVedtakFraArena(fnr)
            var nestSisteBehandlingId: Long? = null
            var nestSisteNyeVedtak: Vedtak? = null
            var behandlingType = BehandlingType.FORSTEGANGSBEHANDLING
            var relatertFagsystem: Fagsystem? = null

            if (eksisterendeVedtak.size > 1) {
                val oppfolgingsperiodeStartDato = oppfolgingsperiode.get().startDato.toLocalDateTime()

                val filteredVedtak =
                    eksisterendeVedtak.filter { it.vedtakFattet?.isAfter(oppfolgingsperiodeStartDato) == true && it.id != vedtak.id }

                nestSisteNyeVedtak = filteredVedtak.maxByOrNull { it.id }
                nestSisteBehandlingId = nestSisteNyeVedtak?.id
                relatertFagsystem = Fagsystem.OPPFOLGINGSVEDTAK_14A

            }
            secureLog.info("siste14aVedtakArena: {}", siste14aVedtakArena)
            if (siste14aVedtakArena != null) {
                if (siste14aVedtakArena.fraDato.isAfter(oppfolgingsperiode.get().startDato.toLocalDate())) {
                    if (nestSisteNyeVedtak != null) {
                        if (siste14aVedtakArena.fraDato.isAfter(nestSisteNyeVedtak.vedtakFattet.toLocalDate())) {
                            nestSisteBehandlingId = siste14aVedtakArena.vedtakId
                            relatertFagsystem = Fagsystem.ARENA
                        }
                    }
                }
            }
            if (nestSisteBehandlingId != null) {
                behandlingType = BehandlingType.REVURDERING
            }
            val sakId = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiode.get().uuid).sakId

            val nySakstatistikkrad = SakStatistikk(
                behandlingId = vedtak.id.toBigInteger(),
                aktorId = vedtak.aktorId,
                oppfolgingPeriodeUUID = oppfolgingsperiode.get().uuid,
                relatertBehandlingId = nestSisteBehandlingId?.toBigInteger(), // dersom dette er nr 2 eller mer i oppfolgingsperioden
                relatertFagsystem = relatertFagsystem, // dersom dette er nr 2 eller mer i oppfolgingsperioden,
                sakId = sakId.toString(),
                mottattTid = oppfolgingsperiode.get().startDato.toInstant(),
                registrertTid = vedtak.utkastOpprettet.toInstant(ZoneOffset.UTC),
                ferdigbehandletTid = Instant.now(),
                endretTid = Instant.now(),
                tekniskTid = Instant.now(),
                sakYtelse = SAK_YTELSE,
                behandlingType = behandlingType, // dersom dette er nr 2 eller mer i oppfolgingsperioden blir det REVURDERING,
                behandlingStatus = BehandlingStatus.FATTET, // Når får vedtaket status sendt
                behandlingResultat = vedtak.innsatsgruppe.toBehandlingResultat(),
                behandlingMetode = BehandlingMetode.MANUELL,
                innsatsgruppe = vedtak.innsatsgruppe.toBehandlingResultat(),
                hovedmal = vedtak.hovedmal,
                opprettetAv = vedtak.veilederIdent,
                saksbehandler = vedtak.veilederIdent,
                ansvarligBeslutter = vedtak.veilederIdent, // dersom kvalitetssikrer så blir dette en annen
                ansvarligEnhet = EnhetId.of(vedtak.oppfolgingsenhetId),
                avsender = Fagsystem.OPPFOLGINGSVEDTAK_14A,
                versjon = environmentProperties.naisAppImage
            )

            try {
                sakStatistikkRepository.insertSakStatistikkRad(nySakstatistikkrad)
                bigQueryService.logEvent(nySakstatistikkrad)
            } catch (e: Exception) {
                secureLog.error("Kunne ikke lagre sakstatistikk", e)
            }

        }
    }

    fun hentStatistikkRader(oppfolgingsperiodeUuid: UUID): Boolean {
        val statistikkListe =
            sakStatistikkRepository.hentSakStatistikkListeInnenforOppfolgingsperiode(oppfolgingsperiodeUuid)
        val antallUtkast =
            statistikkListe.stream().filter { item: SakStatistikk -> item.behandlingStatus == BehandlingStatus.UNDER_BEHANDLING }.toList().size
        return antallUtkast == 0
    }
/*
    fun leggTilStatistikkRadUtkast(
        vedtak: Vedtak, behandlingId: Long, aktorId: String, fnr: Fnr, veilederIdent: String, oppfolgingsenhetId: String
    ) {
        //TODO: Hent mottattTid (som er start oppfølgingsperiode på første vedtak, vi må komme tilbake til hva det er ved seinere vedtak i samme periode.
        //TODO: Avsender er en konstant, versjon må hentes fra Docker-image
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)

        if (statistikkPaa) {

            val oppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
            oppfolgingsperiode.ifPresent {
                val mottattTid = if (hentStatistikkRader(oppfolgingsperiode.get().uuid)) {
                    Instant.now()
                } else {
                    veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr).get().startDato.toInstant()
                }

                val sakId = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiode.get().uuid).sakId

                val sakStatistikk = SakStatistikk(
                    aktorId = aktorId,
                    oppfolgingPeriodeUUID = oppfolgingsperiode.get().uuid,
                    behandlingId = behandlingId.toBigInteger(),
                    sakId = sakId.toString(),
                    mottattTid = mottattTid,
                    endretTid = Instant.now(),
                    tekniskTid = Instant.now(),
                    opprettetAv = veilederIdent,
                    ansvarligEnhet = EnhetId.of(oppfolgingsenhetId),
                    avsender = Fagsystem.OPPFOLGINGSVEDTAK_14A,
                    versjon = environmentProperties.naisAppImage,
                    registrertTid = vedtak.utkastOpprettet.toInstant(ZoneOffset.UTC),
                    behandlingType = behandlingType, // dersom dette er nr 2 eller mer i oppfolgingsperioden blir det REVURDERING,,
                    behandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
                    behandlingMetode = BehandlingMetode.MANUELL,
                    saksbehandler = vedtak.veilederIdent
                )
                try {
                    sakStatistikkRepository.insertSakStatistikkRad(sakStatistikk)
                    bigQueryService.logEvent(sakStatistikk)
                } catch (e: Exception) {
                    secureLog.error("Kunne ikke lagre sakstatistikk", e)
                }

            }
        }
    }
    fun leggTilStatistikkRadUtkastSlett(
        behandlingId: Long, aktorId: String, fnr: Fnr, veilederIdent: String, oppfolgingsenhetId: String
    ) {
        //TODO: Hent mottattTid (som er start oppfølgingsperiode på første vedtak, vi må komme tilbake til hva det er ved seinere vedtak i samme periode.
        //TODO: Avsender er en konstant, versjon må hentes fra Docker-image
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)

        if (statistikkPaa) {

            val oppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
            oppfolgingsperiode.ifPresent {
                val mottattTid = if (hentStatistikkRader(oppfolgingsperiode.get().uuid)) {
                    Instant.now()
                } else {
                    veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr).get().startDato.toInstant()
                }

                val sakId = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiode.get().uuid).sakId

                val sakStatistikk = SakStatistikk(
                    aktorId = aktorId,
                    oppfolgingPeriodeUUID = oppfolgingsperiode.get().uuid,
                    behandlingId = behandlingId.toBigInteger(),
                    sakId = sakId.toString(),
                    mottattTid = mottattTid,
                    endretTid = Instant.now(),
                    tekniskTid = Instant.now(),
                    opprettetAv = veilederIdent,
                    ansvarligEnhet = EnhetId.of(oppfolgingsenhetId),
                    avsender = Fagsystem.OPPFOLGINGSVEDTAK_14A,
                    versjon = environmentProperties.naisAppImage
                )
                try {
                    sakStatistikkRepository.insertSakStatistikkRad(sakStatistikk)
                    bigQueryService.logEvent(sakStatistikk)
                } catch (e: Exception) {
                    secureLog.error("Kunne ikke lagre sakstatistikk", e)
                }
            }
        }
    }
    */
}

