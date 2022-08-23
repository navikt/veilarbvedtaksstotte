package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.EksternBrukerId
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toLocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime

@Service
class EnkelReaktiveringService(
    val aktorOppslagClient: AktorOppslagClient,
    val veilarboppfolgingClient: VeilarboppfolgingClient,
    val siste14aVedtakService: Siste14aVedtakService,
    val vedtakRepository: VedtaksstotteRepository,
    val transactor: TransactionTemplate
) {

    data class Reaktivering(val brukerId: EksternBrukerId, val tidspunkt: ZonedDateTime)

    fun behandleEnkeltReaktivertBruker(reaktivering: Reaktivering) {
        val siste14aVedtak = siste14aVedtakService.siste14aVedtak(reaktivering.brukerId) ?: return

        val identer: BrukerIdenter = aktorOppslagClient.hentIdenter(reaktivering.brukerId)

        val gjeldendeOppfolgingsperiode =
            veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(identer.fnr).orElse(null) ?: return

        if (reaktivering.tidspunkt.toLocalDate() != gjeldendeOppfolgingsperiode.startDato.toLocalDate()) {
            return
        }

        if (!siste14aVedtak.fattetDato.isBefore(reaktivering.tidspunkt)) {
            return
        }

        if (siste14aVedtak.fraArena) {
            siste14aVedtakService.republiserKafkaSiste14aVedtak(reaktivering.brukerId)
        } else {
            transactor.executeWithoutResult {
                val sisteVedtak = vedtakRepository.hentSisteVedtak(identer.aktorId)
                if (!sisteVedtak.isGjeldende) {
                    vedtakRepository.settVedtakTilGjeldende(sisteVedtak.id)
                    siste14aVedtakService.republiserKafkaSiste14aVedtak(reaktivering.brukerId)
                }
            }
        }
    }
}
