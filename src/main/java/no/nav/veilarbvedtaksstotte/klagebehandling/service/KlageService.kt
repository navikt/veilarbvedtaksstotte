package no.nav.veilarbvedtaksstotte.klagebehandling.service

import no.nav.veilarbvedtaksstotte.klagebehandling.domene.dto.OpprettKlageRequest
import no.nav.veilarbvedtaksstotte.klagebehandling.repository.KlageRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class KlageService(
    @param:Autowired private val klageRepository: KlageRepository,
) {

    val logger: Logger = LoggerFactory.getLogger(KlageService::class.java)


    fun opprettKlageBehandling(opprettKlageRequest: OpprettKlageRequest) {
        logger.info("Oppretter klagebehandling for vedtakId ${opprettKlageRequest.vedtakId} ")
        klageRepository.upsertKlageBakgrunnsdata(opprettKlageRequest)
    }

}
