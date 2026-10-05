package no.nav.veilarbvedtaksstotte.client.pdf

import no.nav.veilarbvedtaksstotte.client.person.dto.*
import no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog

fun CvInnhold.sanitize(): CvInnhold {
    return this.copy(
        sammendrag = sammendrag?.let { vaskStringForUgyldigeTegn(it) },
        arbeidserfaring = arbeidserfaring?.map { it.sanitize() },
        utdanning = utdanning?.map { it.sanitize() },
        fagdokumentasjoner = fagdokumentasjoner?.map { it.sanitize() },
        godkjenninger = godkjenninger?.map { it.sanitize() },
        annenErfaring = annenErfaring?.map { it.sanitize() },
        kurs = kurs?.map { it.sanitize() },
        sertifikater = sertifikater?.map { it.sanitize() },
        andreGodkjenninger = andreGodkjenninger?.map { it.sanitize() },
    )
}

fun ArbeidserfaringDtoV2.sanitize(): ArbeidserfaringDtoV2 = copy(
    tittel = tittel?.let { vaskStringForUgyldigeTegn(it) },
    arbeidsgiver = arbeidsgiver?.let { vaskStringForUgyldigeTegn(it) },
    sted = sted?.let { vaskStringForUgyldigeTegn(it) },
    beskrivelse = beskrivelse?.let { vaskStringForUgyldigeTegn(it) }
)

fun UtdanningDtoV2.sanitize(): UtdanningDtoV2 = copy(
    tittel = tittel?.let { vaskStringForUgyldigeTegn(it) },
    utdanningsnivaa = utdanningsnivaa?.let { vaskStringForUgyldigeTegn(it) },
    studiested = studiested?.let { vaskStringForUgyldigeTegn(it) },
    beskrivelse = beskrivelse?.let { vaskStringForUgyldigeTegn(it) }
)

fun FagdokumentasjonDtoV2.sanitize(): FagdokumentasjonDtoV2 = copy(
    tittel = tittel?.let { vaskStringForUgyldigeTegn(it) },
)

fun GodkjenningDtoV2.sanitize(): GodkjenningDtoV2 = copy(
    tittel = tittel?.let { vaskStringForUgyldigeTegn(it) },
    utsteder = utsteder?.let { vaskStringForUgyldigeTegn(it) }
)

fun AnnenErfaringDtoV2.sanitize(): AnnenErfaringDtoV2 = copy(
    rolle = rolle?.let { vaskStringForUgyldigeTegn(it) },
    beskrivelse = beskrivelse?.let { vaskStringForUgyldigeTegn(it) }
)

fun KursDtoV2.sanitize(): KursDtoV2 = copy(
    tittel = tittel?.let { vaskStringForUgyldigeTegn(it) },
    arrangor = arrangor?.let { vaskStringForUgyldigeTegn(it) }
)

fun SertifikatDtoV2.sanitize(): SertifikatDtoV2 = copy(
    tittel = tittel?.let { vaskStringForUgyldigeTegn(it) },
    utsteder = utsteder?.let { vaskStringForUgyldigeTegn(it) }
)

fun vaskStringForUgyldigeTegn(input: String, fjernEmoji: Boolean = false): String {
    val erstattedeKodepunkter = mutableListOf<Int>()
    val fjernedeKodepunkter = mutableListOf<Int>()
    val vasketInput = buildString(input.length) {
        // Behandle ett utvidet grafem om gangen, slik at sammensatte emojier erstattes samlet.
        Regex("""\X""").findAll(input).forEach { treff ->
            // Lagre lengden så delresultatet kan erstattes med én rute hvis grafemet inneholder emoji.
            val start = length
            var inneholderEmoji = false
            treff.value.codePoints().forEach { kodepunkt ->
                val tegntype = Character.getType(kodepunkt)
                when {
                    fjernEmoji && (Character.isEmojiPresentation(kodepunkt) || Character.isExtendedPictographic(kodepunkt) || kodepunkt == 0x20E3) -> {
                        inneholderEmoji = true
                        erstattedeKodepunkter.add(kodepunkt)
                    }
                    tegntype == Character.PRIVATE_USE.toInt() -> {
                        append("□")
                        erstattedeKodepunkter.add(kodepunkt)
                    }
                    (tegntype == Character.CONTROL.toInt() || tegntype == Character.FORMAT.toInt()) &&
                        kodepunkt != '\r'.code && kodepunkt != '\n'.code && kodepunkt != '\t'.code -> {
                        // Behold linjeskift og tabulator, men fjern øvrige kontroll- og formateringstegn.
                        fjernedeKodepunkter.add(kodepunkt)
                    }
                    else -> appendCodePoint(kodepunkt)
                }
            }
            if (inneholderEmoji) {
                setLength(start)
                append("□")
            }
        }
    }

    if (erstattedeKodepunkter.isNotEmpty()) {
        // Logg kodepunktene, ikke tekstinnholdet som ble vasket.
        val erstattedeKodepunkterForLogg = erstattedeKodepunkter.joinToString(", ") { "U+" + it.toString(16).uppercase().padStart(4, '0') }
        secureLog.info("Vasket inputstring for pdf og erstattet følgende private-use- og emoji-kodepunkter med □: $erstattedeKodepunkterForLogg (erstattet ${erstattedeKodepunkter.size} kodepunkter)")
    }

    if (fjernedeKodepunkter.isNotEmpty()) {
        val fjernedeKodepunkterForLogg = fjernedeKodepunkter.joinToString(", ") { "U+" + it.toString(16).uppercase().padStart(4, '0') }
        secureLog.info("Vasket inputstring for pdf og fjernet følgende: $fjernedeKodepunkterForLogg (fjernet ${fjernedeKodepunkter.size} kodepunkter)")
    }
    return vasketInput
}
