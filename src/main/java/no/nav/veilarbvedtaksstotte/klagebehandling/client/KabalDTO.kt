package no.nav.veilarbvedtaksstotte.klagebehandling.client

import java.time.LocalDate

data class KabalDTO(
    val type: String = "KLAGE",
    val sakenGjelder: Part,
    val fagsak: Fagsak,
    val kildeReferanse: String, //Teknisk id brukt i avsendersystemet som Kabal vil bruke når de kommuniserer tilbake. Vi kan bruke vedtaksId her.
    val dvhReferanse: String? = null, // hvor får vi denne fra?
    // Etter de er bestemt vil de lage de tekniske implementasjonene som vi trenger.
    val forrigeBehandlendeEnhet: String, // navkontor vedtaket ble fattet
    val tilknyttedeJournalposter: List<TilknyttetJournalpost>,
    val brukersKlageMottattVedtaksinstans: LocalDate,
    val ytelse: String, //Sakens ytelse. Bruker klageinstansen sitt kodeverk. Må avklares hva vi skal sette her.
    val hjemler: List<String>, //fagleder i KA lager et forslag til oss. Må så gåes opp med juristene /fagrådet.
    val kommentar: String? = null, // Kommentarer fra saksbehandler i førsteinstans som ikke er med i oversendelsesbrevet klager mottar.
)

data class Part(
    val id: PartId
)

data class PartId(
    val type: String = "PERSON",
    val verdi: String
)

data class Fagsak( //feltene dere bruker ved journalføring (i Joark) på saken. Fra veilarboppfolging?
    val fagsakId: String, // IKKE journalpostid
    val fagsystem: String
)

data class TilknyttetJournalpost(
    val type: String,
    val journalpostId: String
)
