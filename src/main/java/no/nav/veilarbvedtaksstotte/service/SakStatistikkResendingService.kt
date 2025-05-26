package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingResultat
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingStatus
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository.Companion.SAK_STATISTIKK_TABLE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SakStatistikkResendingService(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val bigQueryService: BigQueryService
) {
    // 23. mai kl 15:50:00
    @Scheduled(cron = "0 50 15 23 5 ?")
    fun resendStatistikk() {
        // steg 0: lage cron-jobb som trigger funksjon på ønsket tidspunkt (kun EN gang)
        // ✓ steg 1: Hent ut alle rader som vi skal endre, eks. alle rader med behandlig_status='AVBRUTT'
        // ✓ steg 2: Vi har en liste av saksstatistikkrader, går gjennom denne og endrer behandling_status til 'AVSLUTTET' på hver rad. OBS! aldri endre behandling_id og endret_tid, disse to i kombinasjon er en nøkkel for hver rad
        // ✓ steg 3: Insert de endrede radene tilbake i databasen
        // ✓ steg 4: Send til BigQuery – bruk listen fra steg 2

        /*
        * Noterer litt her:
        * Før testkjøring i dev er det 14 rader med behandling_status='AVBRUTT' i databasen, med disse sekvensnummerne (behandling_id):
        * 1398 (3054), 1488 (3103), 1540 (3125), 1545 (3035), 1637 (2741), 1651 (3150), 1653 (3187), 1724 (3213), 1781 (3232), 1814 (3246), 1823 (3249), 1825 (3251), 1968 (2476), 2216 (3433)
        * Glemte å legge på logging før jeg testkjøringen.
        *
        * Det dukket opp 15 rader med behandling_status='AVSLUTTET' i databasen etter kjøring, med disse sekvensnummerne (behandling_id):
        * 2224 (3436), 2245 (3054), 2246 (3103), 2247 (3125), 2248 (3035), 2249 (2741), 2250 (3150), 2251 (3187), 2252 (3213), 2253 (3232), 2254 (3246), 2255 (3249), 2256 (3251), 2257 (2476), 2258 (3433)
        * Den første raden her er ny: 2224 (3436), men den kommer jo fra da vi testa på tirsdag!
        * BigQuery har samme antall rader av begge typene.
        *
        * Jeg tror alt ser fint, men vi kan ta en ekstra titt før vi kjører i prod.
         */
        val log: Logger = LoggerFactory.getLogger(SakStatistikkResendingService::class.java)
        log.info("Starter resending av sakstatistikk med behandling_status='AVBRUTT'")

        val parameters = mapOf<String, Any>("BEHANDLING_STATUS" to BehandlingStatus.AVBRUTT.name)

        val sql = "SELECT * FROM $SAK_STATISTIKK_TABLE WHERE behandling_status = :BEHANDLING_STATUS"

        val sakStatistikkRader = sakStatistikkRepository.hentSakStatistikkListe(sql, parameters)

        // Lager de nye radene, pass på å slette/ikke sette sekvensnummer, det settes automatisk ved innsetting i databasen
        val endredeRader = sakStatistikkRader.map { it ->
            it.copy(
                sekvensnummer = null,
                behandlingStatus = BehandlingStatus.AVSLUTTET,
                behandlingResultat = BehandlingResultat.AVBRUTT
            )
        }

        val lagredeDatabaseRader = sakStatistikkRepository.insertSakStatistikkRadBatch(endredeRader)

        bigQueryService.logEvent(lagredeDatabaseRader)
        log.info("Resending av sakstatistikk med behandling_status='AVBRUTT' ferdig")
    }

}