package no.nav.veilarbvedtaksstotte.client.dokument

import no.nav.common.health.HealthCheck

interface VeilarbdokumentClient : HealthCheck {
    fun sendDokument(sendDokumentDTO: SendDokumentDTO): DokumentSendtDTO
    fun produserDokumentUtkast(sendDokumentDTO: SendDokumentDTO): ByteArray
    fun produserDokumentV2(produserDokumentV2DTO: ProduserDokumentV2DTO): ByteArray
}
