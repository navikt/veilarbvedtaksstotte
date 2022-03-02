package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostDTO
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DistribusjonServiceV2(val vedtaksstotteRepository: VedtaksstotteRepository,
                            val dokdistribusjonClient: DokdistribusjonClient) {

    val log = LoggerFactory.getLogger(DistribusjonServiceV2::class.java)

    fun distribuerVedtak(vedtakId: Long) {
        // Oppdaterer vedtak til "sender" tilstand for å redusere risiko for dupliserte utsendelser av dokument.
        vedtaksstotteRepository.oppdaterSender(vedtakId, true)
        try {
            val vedtak = vedtaksstotteRepository.hentVedtak(vedtakId)
            validerVedtakForDistribusjon(vedtak)
            val distribusjonBestillingId: DistribusjonBestillingId =
                distribuerJournalpost(vedtak.journalpostId)
            vedtaksstotteRepository.lagreDokumentbestillingsId(vedtakId, distribusjonBestillingId)
        } catch (e: Exception) {
            try {
                vedtaksstotteRepository.oppdaterSender(vedtakId, false)
            } catch (e2: Exception) {
                log.error("Kunne ikke oppdatere sender til false", e2)
            }
            throw e
        }
    }

    fun validerVedtakForDistribusjon(vedtak: Vedtak) {
        check(vedtak.journalpostId != null && vedtak.dokumentInfoId != null) {
            String.format(
                "Kan ikke distribuere vedtak med id %s som mangler journalpostId(%s) og/eller dokumentinfoId(%s)",
                vedtak.id, vedtak.journalpostId, vedtak.dokumentInfoId
            )
        }
        check(vedtak.dokumentbestillingId == null) {
            String.format(
                "Kan ikke distribuere vedtak med id %s som allerede har en dokumentbestillingId(%s)",
                vedtak.id, vedtak.dokumentbestillingId
            )
        }
    }

    fun distribuerJournalpost(jounralpostId: String): DistribusjonBestillingId {
        try {
            val respons = dokdistribusjonClient.distribuerJournalpost(
                DistribuerJournalpostDTO(
                    journalpostId = jounralpostId,
                    bestillendeFagsystem = "BD11", // veilarb-kode
                    dokumentProdApp = "VEILARB_VEDTAK14A" // for sporing og feilsøking
                )
            )
            return if (respons?.bestillingsId != null) {
                log.info("Distribusjon for journalpost med journalpostId=$jounralpostId bestilt med bestillingsId=${respons.bestillingsId}");
                DistribusjonBestillingId.Uuid(respons.bestillingsId)
            } else {
                log.error("Ikke forventet respons fra bestilling av distribusjon for journalpost med journalpostId=$jounralpostId. bestillingsId settes til ${DistribusjonBestillingId.Feilet} og må rettes manuelt.")
                DistribusjonBestillingId.Feilet
            }
        } catch (e: RuntimeException) {
            log.error("Distribusjon av journalpost med journalpostId=$jounralpostId feilet", e);
            throw e;
        }
    }
}
