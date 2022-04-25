package no.nav.veilarbvedtaksstotte.client.dokument

import no.nav.common.health.HealthCheck

interface VeilarbdokumentClient : HealthCheck {
    fun produserDokument(produserDokumentDTO: ProduserDokumentDTO): ByteArray
}
