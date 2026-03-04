package no.nav.veilarbvedtaksstotte.klagebehandling.client

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class KabalDTO(
    val type: String = "KLAGE",
    val sakenGjelder: Part,
    //val klager: Part, // dersom klagen er levert av noen andre enn den det gjelder
    val fagsak: Fagsak,
    val kildeReferanse: String, //Teknisk id brukt i avsendersystemet som Kabal vil bruke når de kommuniserer tilbake. Vi kan bruke vedtaksId her.
    //val dvhReferanse: String? = null, // hvor får vi denne fra?
    val hjemler: List<String>, //fagleder i KA lager et forslag til oss. Må så gåes opp med juristene /fagrådet.
    // Etter de er bestemt vil de lage de tekniske implementasjonene som vi trenger.
    val forrigeBehandlendeEnhet: String, // navkontor vedtaket ble fattet
    val tilknyttedeJournalposter: List<TilknyttetJournalpost>, // to styk, vedtaket og klagen, se under
    @field:JsonFormat(pattern = "yyyy-MM-dd")
    val brukersKlageMottattVedtaksinstans: LocalDate, // datoen veileder fyller inn
    val ytelse: String, //Sakens ytelse. Bruker KA sitt kodeverk. Må avklares hva vi skal sette her.
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
    val type: String, // aktuelle enums: OPPRINNELIG_VEDTAK, BRUKERS_KLAGE
    val journalpostId: String
)
