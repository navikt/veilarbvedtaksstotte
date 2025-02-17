package no.nav.veilarbvedtaksstotte.service

import io.getunleash.DefaultUnleash
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.api.dto.response.Diskresjonskode
import no.nav.poao_tilgang.client.PoaoTilgangClient
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
import java.time.temporal.ChronoUnit


@Service
class SakStatistikkService @Autowired constructor(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val aktorOppslagClient: AktorOppslagClient,
    private val bigQueryService: BigQueryService,
    private val unleashClient: DefaultUnleash,
    private val environmentProperties: EnvironmentProperties,
    private val poaoTilgangClient: PoaoTilgangClient
) {
    fun fattetVedtak(vedtak: Vedtak, fnr: Fnr) {
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        if (statistikkPaa) {
            val statistikkRad = SakStatistikk(
                ferdigbehandletTid = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                behandlingStatus = BehandlingStatus.FATTET,
                behandlingMetode = if(vedtak.beslutterIdent != null) BehandlingMetode.TOTRINNS else BehandlingMetode.MANUELL,
                ansvarligBeslutter = vedtak.beslutterIdent
            )

            val populertMedStatiskeData = populerSakstatistikkMedStatiskeData(statistikkRad)
            val populertMedVedtaksdata = populerSakstatistikkMedVedtakData(populertMedStatiskeData, vedtak)
            val populertMedOppfolgingsperiodeData = populerSakStatistikkMedOppfolgingsperiodeData(populertMedVedtaksdata, fnr)
            val ferdigpopulertStatistikkRad = sjekkOmPersonErKode6(fnr, populertMedOppfolgingsperiodeData)

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
            val populertMedOppfolgingsperiodeData = populerSakStatistikkMedOppfolgingsperiodeData(populertMedVedtaksdata, fnr)
            val ferdigpopulertStatistikkRad = sjekkOmPersonErKode6(fnr, populertMedOppfolgingsperiodeData)

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
        if (statistikkPaa) {
            val aktorId = AktorId(vedtak.aktorId)
            val fnr = aktorOppslagClient.hentFnr(aktorId)

            val populertMedStatiskeData = populerSakstatistikkMedStatiskeData(SakStatistikk())
            val populertMedVedtaksdata = populerSakstatistikkMedVedtakData(populertMedStatiskeData, vedtak)
            val populertMedOppfolgingsperiodeData = populerSakStatistikkMedOppfolgingsperiodeData(populertMedVedtaksdata, fnr)
            val populertMedKode6Sjekk = sjekkOmPersonErKode6(fnr, populertMedOppfolgingsperiodeData)

            val ferdigpopulertStatistikkRad = populertMedKode6Sjekk.copy(
                innsatsgruppe = null,
                hovedmal = null,
                behandlingResultat = null,
                behandlingStatus = BehandlingStatus.AVBRUTT,
                behandlingMetode = BehandlingMetode.MANUELL,
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

    fun startetKvalitetssikring (vedtak: Vedtak) {
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        if (statistikkPaa) {
            val aktorId = AktorId(vedtak.aktorId)
            val fnr = aktorOppslagClient.hentFnr(aktorId)

            val statistikkRad = SakStatistikk(
                behandlingStatus = BehandlingStatus.SENDT_TIL_KVALITETSSIKRING,
                behandlingMetode = BehandlingMetode.TOTRINNS,
            )
            val populertMedStatiskeData = populerSakstatistikkMedStatiskeData(statistikkRad)
            val populertMedVedtaksdata = populerSakstatistikkMedVedtakData(populertMedStatiskeData, vedtak)
            val populertMedOppfolgingsperiodeData = populerSakStatistikkMedOppfolgingsperiodeData(populertMedVedtaksdata, fnr)
            val ferdigpopulertStatistikkRad = sjekkOmPersonErKode6(fnr, populertMedOppfolgingsperiodeData)


            try {
                ferdigpopulertStatistikkRad.validate()
                sakStatistikkRepository.insertSakStatistikkRad(ferdigpopulertStatistikkRad)
                bigQueryService.logEvent(ferdigpopulertStatistikkRad)
            } catch (e: Exception) {
                secureLog.error("Kunne ikke lagre startetKvalitetssikring - sakstatistikk", e)
            }
        }
    }

    fun bliEllerTaOverSomKvalitetssikrer(vedtak: Vedtak, innloggetVeileder: String) {
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        if (statistikkPaa) {
            val aktorId = AktorId(vedtak.aktorId)
            val fnr = aktorOppslagClient.hentFnr(aktorId)

            val statistikkRad = SakStatistikk(
                behandlingStatus = BehandlingStatus.SENDT_TIL_KVALITETSSIKRING,
                behandlingMetode = BehandlingMetode.TOTRINNS,
                ansvarligBeslutter = innloggetVeileder,
            )
            val populertMedStatiskeData = populerSakstatistikkMedStatiskeData(statistikkRad)
            val populertMedVedtaksdata = populerSakstatistikkMedVedtakData(populertMedStatiskeData, vedtak)
            val populertMedOppfolgingsperiodeData = populerSakStatistikkMedOppfolgingsperiodeData(populertMedVedtaksdata, fnr)
            val ferdigpopulertStatistikkRad = sjekkOmPersonErKode6(fnr, populertMedOppfolgingsperiodeData)


            try {
                ferdigpopulertStatistikkRad.validate()
                sakStatistikkRepository.insertSakStatistikkRad(ferdigpopulertStatistikkRad)
                bigQueryService.logEvent(ferdigpopulertStatistikkRad)
            } catch (e: Exception) {
                secureLog.error("Kunne ikke lagre bliEllerTaOverSomKvalitetssikrer - sakstatistikk", e)
            }
        }
    }

    fun avbrytKvalitetssikringsprosess(vedtak: Vedtak) {
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        if (statistikkPaa) {
            val aktorId = AktorId(vedtak.aktorId)
            val fnr = aktorOppslagClient.hentFnr(aktorId)

            val populertMedStatiskeData = populerSakstatistikkMedStatiskeData(SakStatistikk())
            val populertMedVedtaksdata = populerSakstatistikkMedVedtakData(populertMedStatiskeData, vedtak)
            val populertMedOppfolgingsperiodeData = populerSakStatistikkMedOppfolgingsperiodeData(populertMedVedtaksdata, fnr)
            val populertMedKode6Sjekk = sjekkOmPersonErKode6(fnr, populertMedOppfolgingsperiodeData)

            val ferdigpopulertStatistikkRad = populertMedKode6Sjekk.copy(
                ansvarligBeslutter = null,
                behandlingResultat = null,
                hovedmal = null,
                innsatsgruppe = null,
                behandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
                behandlingMetode = BehandlingMetode.MANUELL,
            )

            try {
                ferdigpopulertStatistikkRad.validate()
                sakStatistikkRepository.insertSakStatistikkRad(ferdigpopulertStatistikkRad)
                bigQueryService.logEvent(ferdigpopulertStatistikkRad)
            } catch (e: Exception) {
                secureLog.error("Kunne ikke lagre avbrytKvalitetssikringsprosess - sakstatistikk", e)
            }
        }
    }

    fun overtattUtkast(vedtak: Vedtak, innloggetVeilederIdent: String, erAlleredeBeslutter: Boolean) {
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)
        if (statistikkPaa) {
            val aktorId = AktorId(vedtak.aktorId)
            val fnr = aktorOppslagClient.hentFnr(aktorId)

            val populertMedStatiskeData = populerSakstatistikkMedStatiskeData(SakStatistikk())
            val populertMedVedtaksdata = populerSakstatistikkMedVedtakData(populertMedStatiskeData, vedtak)
            val populertMedOppfolgingsperiodeData = populerSakStatistikkMedOppfolgingsperiodeData(populertMedVedtaksdata, fnr)
            val populertMedKode6Sjekk = sjekkOmPersonErKode6(fnr, populertMedOppfolgingsperiodeData)

            val ferdigpopulertStatistikkRad = populertMedKode6Sjekk.copy(
                ansvarligBeslutter = if (erAlleredeBeslutter) innloggetVeilederIdent else vedtak.beslutterIdent,
                behandlingStatus = if (vedtak.beslutterProsessStatus != null) BehandlingStatus.SENDT_TIL_KVALITETSSIKRING else BehandlingStatus.UNDER_BEHANDLING,
                behandlingMetode = if (vedtak.beslutterProsessStatus != null) BehandlingMetode.TOTRINNS else BehandlingMetode.MANUELL,
                saksbehandler = innloggetVeilederIdent
            )

            try {
                ferdigpopulertStatistikkRad.validate()
                sakStatistikkRepository.insertSakStatistikkRad(ferdigpopulertStatistikkRad)
                bigQueryService.logEvent(ferdigpopulertStatistikkRad)
            } catch (e: Exception) {
                secureLog.error("Kunne ikke lagre overtattUtkast - sakstatistikk", e)
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
            registrertTid = vedtak.utkastOpprettet?.toInstant(ZoneOffset.of("+01:00"))?.truncatedTo(ChronoUnit.SECONDS),
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
            mottattTid = if (tidligereVedtakIOppfolgingsperioden != null) sakStatistikk.registrertTid else oppfolgingsperiode.get().startDato.toInstant().truncatedTo(ChronoUnit.SECONDS),
            sakId = sakId.toString(),

            relatertBehandlingId = tidligereVedtakIOppfolgingsperioden?.id,
            relatertFagsystem =  tidligereVedtakIOppfolgingsperioden?.let { relatertFagsystem },
            behandlingType = if (tidligereVedtakIOppfolgingsperioden != null) BehandlingType.REVURDERING else BehandlingType.FORSTEGANGSBEHANDLING
        )
    }

    private fun sjekkOmPersonErKode6(fnr: Fnr, sakStatistikk: SakStatistikk): SakStatistikk {
        val tilgangsattributterResponse = poaoTilgangClient.hentTilgangsAttributter(fnr.get()).getOrThrow()
        secureLog.error("Fått diskresjonskode fra poao-tilgang: ${tilgangsattributterResponse.diskresjonskode}")
        if (tilgangsattributterResponse.diskresjonskode === Diskresjonskode.STRENGT_FORTROLIG || tilgangsattributterResponse.diskresjonskode === Diskresjonskode.STRENGT_FORTROLIG_UTLAND) {
            return sakStatistikk.copy(
                opprettetAv = "-5",
                saksbehandler = "-5",
                ansvarligBeslutter = if(sakStatistikk.ansvarligBeslutter != null ) "-5" else null,
                ansvarligEnhet = EnhetId.of("-5")
            )
        }
        return sakStatistikk
    }
}

