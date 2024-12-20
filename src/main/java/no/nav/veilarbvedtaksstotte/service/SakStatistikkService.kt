package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import io.getunleash.DefaultUnleash
import no.nav.veilarbvedtaksstotte.utils.SAK_STATISTIKK_PAA
import java.util.UUID

private const val AVSENDER = "Oppfølgingsvedtak § 14 a"

@Service
class SakStatistikkService @Autowired constructor(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val unleashClient: DefaultUnleash
) {

    fun hentStatistikkRader(oppfolgingsperiodeUuid: UUID): Boolean {
        val statistikkListe =
            sakStatistikkRepository.hentSakStatistikkListeInnenforOppfolgingsperiode(oppfolgingsperiodeUuid)
        val antallUtkast =
            statistikkListe.stream()
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
                val sakStatistikk = SakStatistikk(
                    aktorId = aktorId,
                    oppfolgingPeriodeUUID = oppfolgingsperiode.get().uuid,
                    behandlingId = behandlingId.toBigInteger(),
                    sakId = sakId.toString(),
                    mottattTid = mottattTid,
                    endretTid = LocalDateTime.now(),
                    tekniskTid = LocalDateTime.now(),
                    behandlingType = SakStatistikk.BehandlingType.VEDTAK.name,
                    behandlingStatus = SakStatistikk.BehandlingStatus.UTKAST.name,
                    behandlingMetode = SakStatistikk.BehandlingMetode.MANUELL.name,
                    opprettetAv = veilederIdent,
                    ansvarligEnhet = oppfolgingsenhetId,
                    avsender = AVSENDER,
                    versjon = "Dockerimage_tag_1"
                )
                sakStatistikkRepository.insertSakStatistikkRad(sakStatistikk)
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
                val sakStatistikk = SakStatistikk(
                    aktorId = aktorId,
                    oppfolgingPeriodeUUID = oppfolgingsperiode.get().uuid,
                    behandlingId = behandlingId.toBigInteger(),
                    sakId = sakId.toString(),
                    mottattTid = mottattTid,
                    endretTid = LocalDateTime.now(),
                    tekniskTid = LocalDateTime.now(),
                    behandlingType = SakStatistikk.BehandlingType.VEDTAK.name,
                    behandlingStatus = SakStatistikk.BehandlingStatus.AVBRUTT.name,
                    behandlingMetode = SakStatistikk.BehandlingMetode.MANUELL.name,
                    opprettetAv = veilederIdent,
                    ansvarligEnhet = oppfolgingsenhetId,
                    avsender = AVSENDER,
                    versjon = "Dockerimage_tag_1"
                )
                sakStatistikkRepository.insertSakStatistikkRad(sakStatistikk)
            }
        }
    }
}

