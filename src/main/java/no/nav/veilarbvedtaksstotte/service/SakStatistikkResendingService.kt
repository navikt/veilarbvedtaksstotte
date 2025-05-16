package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingResultat
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingStatus
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository.Companion.SAK_STATISTIKK_TABLE
import org.springframework.stereotype.Service

@Service
class SakStatistikkResendingService (private val sakStatistikkRepository: SakStatistikkRepository) {
    fun resendStatistikk() {
        // steg 0: lage cron-jobb som trigger funksjon på ønsket tidspunkt (kun EN gang)
        // :checkmark: steg 1: Hent ut alle rader som vi skal endre, eks. alle rader med behandlig_status='AVBRUTT'
        // :checkmark: steg 2: Vi har en liste av saksstatistikkrader, går gjennom denne og endrer behandling_status til 'AVSLUTTET' på hver rad. OBS! aldri endre behandling_id og endret_tid, disse to i kombinasjon er en nøkkel for hver rad
        // steg 3: Insert de endrede radene tilbake i databasen
        // steg 4: Send til BigQuery – bruk listen fra steg 2

        val parameters = mapOf<String, Any>("BEHANDLING_STATUS" to BehandlingStatus.AVBRUTT)

        val sql = "SELECT * FROM $SAK_STATISTIKK_TABLE WHERE behandling_status = :BEHANDLING_STATUS"

        val sakStatistikkRader = sakStatistikkRepository.hentSakStatistikkListe(sql, parameters)

        // Lager de nye radene, pass på å slette/ikke sette sekvensnummer, det settes automatisk ved innsetting i databasen
        sakStatistikkRader.map { it -> it.copy(sekvensnummer = null, behandlingStatus = BehandlingStatus.AVSLUTTET, behandlingResultat = BehandlingResultat.AVBRUTT ) }
    }

}