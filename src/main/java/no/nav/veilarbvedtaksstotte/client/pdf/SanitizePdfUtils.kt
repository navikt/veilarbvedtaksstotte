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

fun vaskStringForUgyldigeTegn(input: String): String {
    val privateUseTegnRegex = Regex("""\p{Co}""")
    val privateUseKodepunkter = privateUseTegnRegex.findAll(input).map { it.value.codePointAt(0) }.toList()
    val inputMedErstattedePrivateUseTegn = privateUseTegnRegex.replace(input, "□")

    if (privateUseKodepunkter.isNotEmpty()) {
        val privateUseKodepunkterForLogg = privateUseKodepunkter.joinToString(", ") { "U+" + it.toString(16).uppercase().padStart(4, '0') }
        secureLog.info("Vasket inputstring for pdf og erstattet følgende private-use-tegn med □: $privateUseKodepunkterForLogg (erstattet ${privateUseKodepunkter.size} tegn)")
    }

    val kontrollOgFormatTegnRegex = Regex("""[\p{Cc}\p{Cf}&&[^\r\n\t]]""")
    val vasketInput = kontrollOgFormatTegnRegex.replace(inputMedErstattedePrivateUseTegn, "")

    val fjernedeKodepunkterForLogg = kontrollOgFormatTegnRegex.findAll(inputMedErstattedePrivateUseTegn).map { it.value.codePointAt(0) }
        .joinToString(", ") { "U+" + it.toString(16).uppercase().padStart(4, '0') }

    val antallUtf16KodeenheterFjernet = inputMedErstattedePrivateUseTegn.length - vasketInput.length

    if (antallUtf16KodeenheterFjernet > 0) {
        secureLog.info("Vasket inputstring for pdf og fjernet følgende: $fjernedeKodepunkterForLogg (fjernet $antallUtf16KodeenheterFjernet tegn)")
    }
    return vasketInput
}
