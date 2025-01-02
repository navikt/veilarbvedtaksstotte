package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import io.getunleash.DefaultUnleash
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.SAK_STATISTIKK_PAA
import java.util.UUID

private const val AVSENDER = "Oppfølgingsvedtak § 14 a"

@Service
class SakStatistikkService @Autowired constructor(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val unleashClient: DefaultUnleash
) {
    private final val vedtaksstotteRepository: VedtaksstotteRepository = TODO("initialize me")

    fun fyllSakStatistikk(
        aktorId: String,
        oppfolgingsperiodeUuid: UUID,
        behandlingId: Long,
        sakId: Long,
        mottattTid: LocalDateTime,
        registrertTid: LocalDateTime,
        ferdigbehandletTid: LocalDateTime,
        endretTid: LocalDateTime,
        tekniskTid: LocalDateTime,
        behandlingType: SakStatistikk.BehandlingType,
        behandlingStatus: SakStatistikk.BehandlingStatus,
        behandlingMetode: SakStatistikk.BehandlingMetode,
        opprettetAv: String,
        ansvarligEnhet: String,
        avsender: String,
        versjon: String
    ): SakStatistikk {

        val sakStatistikk = SakStatistikk(
            aktorId = aktorId,
            oppfolgingPeriodeUUID = oppfolgingsperiodeUuid,
            behandlingId = behandlingId.toBigInteger(),
            sakId = sakId.toString(),
            mottattTid = mottattTid,
            endretTid = endretTid,
            tekniskTid = tekniskTid,
            behandlingType = behandlingType.name,
            behandlingStatus = behandlingStatus.name,
            behandlingMetode = behandlingMetode.name,
            opprettetAv = opprettetAv,
            ansvarligEnhet = ansvarligEnhet,
            avsender = avsender,
            versjon = versjon
        )
        return sakStatistikk
    }

    fun hentStatistikkRader(oppfolgingsperiodeUuid: UUID): Boolean {
        val statistikkListe =
            sakStatistikkRepository.hentSakStatistikkListeInnenforOppfolgingsperiode(oppfolgingsperiodeUuid)
        val antallUtkast = statistikkListe.stream()
            .filter { item: SakStatistikk -> item.behandlingStatus == SakStatistikk.BehandlingStatus.UTKAST.name }
            .toList().size
        return antallUtkast == 0
    }

    fun leggTilStatistikkRadUtkast(
        behandlingId: Long, aktorId: String, fnr: Fnr, veilederIdent: String, oppfolgingsenhetId: String
    ) {
        //TODO: Hent mottattTid (som er start oppfølgingsperiode på første vedtak, vi må komme tilbake til hva det er ved seinere vedtak i samme periode.
        //TODO: Avsender er en konstant, versjon må hentes fra Docker-image
        val statistikkPaa = unleashClient.isEnabled(SAK_STATISTIKK_PAA)

        if (statistikkPaa) {

            val oppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
            oppfolgingsperiode.ifPresent {
                val mottattTid = if (hentStatistikkRader(oppfolgingsperiode.get().uuid)) {
                    LocalDateTime.now()
                } else {
                    veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr).get().startDato.toLocalDateTime()
                }


                val sakId = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiode.get().uuid).sakId

                sakStatistikkRepository.insertSakStatistikkRad(
                    fyllSakStatistikk(
                        aktorId,
                        oppfolgingsperiode.get().uuid,
                        behandlingId,
                        sakId,
                        mottattTid,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        SakStatistikk.BehandlingType.VEDTAK,
                        SakStatistikk.BehandlingStatus.UTKAST,
                        SakStatistikk.BehandlingMetode.MANUELL,
                        veilederIdent,
                        oppfolgingsenhetId,
                        AVSENDER,
                        "Dockerimage_tag_1"
                    )
                )
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
                    LocalDateTime.now()
                } else {
                    veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr).get().startDato.toLocalDateTime()
                }

                val sakId = veilarboppfolgingClient.hentOppfolgingsperiodeSak(oppfolgingsperiode.get().uuid).sakId

                val utkast = vedtaksstotteRepository.hentUtkast(aktorId)

                sakStatistikkRepository.insertSakStatistikkRad(
                    fyllSakStatistikk(
                        aktorId,
                        oppfolgingsperiode.get().uuid,
                        behandlingId,
                        sakId,
                        mottattTid,
                        utkast.utkastOpprettet,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        SakStatistikk.BehandlingType.VEDTAK,
                        SakStatistikk.BehandlingStatus.AVBRUTT,
                        SakStatistikk.BehandlingMetode.MANUELL,
                        veilederIdent,
                        oppfolgingsenhetId,
                        AVSENDER,
                        "Dockerimage_tag_1"
                    )
                )
            }
        }
    }
}

