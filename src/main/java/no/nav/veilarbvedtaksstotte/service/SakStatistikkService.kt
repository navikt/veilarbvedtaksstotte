package no.nav.veilarbvedtaksstotte.service

import io.getunleash.DefaultUnleash
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.config.EnvironmentProperties
import no.nav.veilarbvedtaksstotte.domain.statistikk.*
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import no.nav.veilarbvedtaksstotte.utils.SAK_STATISTIKK_PAA
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset


@Service
class SakStatistikkService @Autowired constructor(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val aktorOppslagClient: AktorOppslagClient,
    private val bigQueryService: BigQueryService,
    private val unleashClient: DefaultUnleash,
    private val environmentProperties: EnvironmentProperties
) {
    fun fattetVedtak(vedtak: Vedtak, fnr: Fnr) {
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        if (statistikkPaa) {
            val statistikkRad = SakStatistikk(
                ferdigbehandletTid = Instant.now(),
                behandlingStatus = BehandlingStatus.FATTET,
                behandlingMetode = BehandlingMetode.MANUELL,
            )

            val populertMedStatiskeData = populerSakstatistikkMedStatiskeData(statistikkRad)
            val populertMedVedtaksdata = populerSakstatistikkMedVedtakData(populertMedStatiskeData, vedtak)
            val ferdigpopulertStatistikkRad = populerSakStatistikkMedOppfolgingsperiodeData(populertMedVedtaksdata, fnr)

            try {
                ferdigpopulertStatistikkRad.validate()
                sakStatistikkRepository.insertSakStatistikkRad(ferdigpopulertStatistikkRad)
                bigQueryService.logEvent(ferdigpopulertStatistikkRad)
            } catch (e: Exception) {
                secureLog.error("Kunne ikke lagre fattetvedtak - sakstatistikk", e)
            }
        }
    }

    fun opprettetUtkast(
        vedtak: Vedtak, fnr: Fnr
    ) {
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        if (statistikkPaa) {
            val statistikkRad = SakStatistikk(
                behandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
                behandlingMetode = BehandlingMetode.MANUELL,
            )

            val populertMedStatiskeData = populerSakstatistikkMedStatiskeData(statistikkRad)
            val populertMedVedtaksdata = populerSakstatistikkMedVedtakData(populertMedStatiskeData, vedtak)
            val ferdigpopulertStatistikkRad = populerSakStatistikkMedOppfolgingsperiodeData(populertMedVedtaksdata, fnr)

            try {
                ferdigpopulertStatistikkRad.validate()
                sakStatistikkRepository.insertSakStatistikkRad(ferdigpopulertStatistikkRad)
                bigQueryService.logEvent(ferdigpopulertStatistikkRad)
            } catch (e: Exception) {
                secureLog.error("Kunne ikke lagre opprettutkast - sakstatistikk", e)
            }
        }
    }
    fun slettetUtkast(
        vedtak: Vedtak
    ) {
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        val aktorId = AktorId(vedtak.aktorId)
        val fnr = aktorOppslagClient.hentFnr(aktorId)
        if (statistikkPaa) {
            val statistikkRad = SakStatistikk(
                behandlingStatus = BehandlingStatus.AVBRUTT,
                behandlingMetode = BehandlingMetode.MANUELL,
            )

            val populertMedStatiskeData = populerSakstatistikkMedStatiskeData(statistikkRad)
            val populertMedVedtaksdata = populerSakstatistikkMedVedtakData(populertMedStatiskeData, vedtak)
            val populertMedOppfolgingsperiodeData = populerSakStatistikkMedOppfolgingsperiodeData(populertMedVedtaksdata, fnr)

            val ferdigpopulertStatistikkRad = populertMedOppfolgingsperiodeData.copy(
                innsatsgruppe = null,
                hovedmal = null,
                behandlingResultat = null
            )

            try {
                ferdigpopulertStatistikkRad.validate()
                sakStatistikkRepository.insertSakStatistikkRad(ferdigpopulertStatistikkRad)
                bigQueryService.logEvent(ferdigpopulertStatistikkRad)
            } catch (e: Exception) {
                secureLog.error("Kunne ikke lagre slettetutkast - sakstatistikk", e)
            }
        }
    }

    private fun populerSakstatistikkMedStatiskeData(sakStatistikk: SakStatistikk): SakStatistikk {
        return sakStatistikk.copy(
            endretTid = Instant.now(),
            tekniskTid = Instant.now(),
            sakYtelse = SAK_YTELSE,
            avsender = Fagsystem.OPPFOLGINGSVEDTAK_14A,
            versjon = environmentProperties.naisAppImage
        )
    }

    private fun populerSakstatistikkMedVedtakData(sakStatistikk: SakStatistikk, vedtak: Vedtak): SakStatistikk {
        return sakStatistikk.copy(
            aktorId = AktorId.of(vedtak.aktorId),
            behandlingId = vedtak.id.toBigInteger(),
            registrertTid = vedtak.utkastOpprettet?.toInstant(ZoneOffset.UTC),
            behandlingResultat = vedtak.innsatsgruppe?.toBehandlingResultat(),
            innsatsgruppe = vedtak.innsatsgruppe?.toBehandlingResultat(),
            hovedmal = vedtak.hovedmal?.let { HovedmalNy.valueOf(it.toString())},
            opprettetAv = vedtak.veilederIdent,
            saksbehandler = vedtak.veilederIdent,
            ansvarligEnhet = vedtak.oppfolgingsenhetId?.let { EnhetId.of(it) },
        )
    }
    private fun populerSakStatistikkMedOppfolgingsperiodeData(sakStatistikk: SakStatistikk, fnr: Fnr): SakStatistikk {
        val oppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
        val sakId = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiode.get().uuid).sakId

        val tidligereVedtakIOppfolgingsperioden = sakStatistikkRepository.hentForrigeVedtakFraSammeOppfolgingsperiode(oppfolgingsperiode.get().startDato, sakStatistikk.aktorId!!, fnr, sakStatistikk.behandlingId!!)
        val relatertFagsystem = if (tidligereVedtakIOppfolgingsperioden != null && tidligereVedtakIOppfolgingsperioden.fraArena) Fagsystem.ARENA else Fagsystem.OPPFOLGINGSVEDTAK_14A

        return sakStatistikk.copy(
            oppfolgingPeriodeUUID = oppfolgingsperiode.get().uuid,
            mottattTid = if (tidligereVedtakIOppfolgingsperioden != null) sakStatistikk.registrertTid else oppfolgingsperiode.get().startDato.toInstant(), //TODO må gjøre noe med datoen mtp UTC her

            sakId = sakId.toString(),

            relatertBehandlingId = tidligereVedtakIOppfolgingsperioden?.id,
            relatertFagsystem =  tidligereVedtakIOppfolgingsperioden?.let { relatertFagsystem },
            behandlingType = if (tidligereVedtakIOppfolgingsperioden != null) BehandlingType.REVURDERING else BehandlingType.FORSTEGANGSBEHANDLING
        )
    }
}

