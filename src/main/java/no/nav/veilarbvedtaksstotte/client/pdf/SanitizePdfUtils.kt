package no.nav.veilarbvedtaksstotte.client.pdf

import no.nav.veilarbvedtaksstotte.client.person.dto.*

fun CvInnhold.sanitize(sanitizer: (String) -> String): CvInnhold {
    return this.copy(
        sammendrag = sammendrag?.let { sanitizer(it) },
        arbeidserfaring = arbeidserfaring?.map { it.sanitize(sanitizer) },
        utdanning = utdanning?.map { it.sanitize(sanitizer) },
        fagdokumentasjoner = fagdokumentasjoner?.map { it.sanitize(sanitizer) },
        godkjenninger = godkjenninger?.map { it.sanitize(sanitizer) },
        annenErfaring = annenErfaring?.map { it.sanitize(sanitizer) },
        kurs = kurs?.map { it.sanitize(sanitizer) },
        sertifikater = sertifikater?.map { it.sanitize(sanitizer) },
        andreGodkjenninger = andreGodkjenninger?.map { it.sanitize(sanitizer) },
    )
}

fun ArbeidserfaringDtoV2.sanitize(sanitizer: (String) -> String): ArbeidserfaringDtoV2 = copy(
    tittel = tittel?.let { sanitizer(it) },
    arbeidsgiver = arbeidsgiver?.let { sanitizer(it) },
    sted = sted?.let { sanitizer(it) },
    beskrivelse = beskrivelse?.let { sanitizer(it) }
)

fun UtdanningDtoV2.sanitize(sanitizer: (String) -> String): UtdanningDtoV2 = copy(
    tittel = tittel?.let { sanitizer(it) },
    utdanningsnivaa = utdanningsnivaa?.let { sanitizer(it) },
    studiested = studiested?.let { sanitizer(it) },
    beskrivelse = beskrivelse?.let { sanitizer(it) }
)

fun FagdokumentasjonDtoV2.sanitize(sanitizer: (String) -> String): FagdokumentasjonDtoV2 = copy(
    tittel = tittel?.let { sanitizer(it) },
)

fun GodkjenningDtoV2.sanitize(sanitizer: (String) -> String): GodkjenningDtoV2 = copy(
    tittel = tittel?.let { sanitizer(it) },
    utsteder = utsteder?.let { sanitizer(it) }
)

fun AnnenErfaringDtoV2.sanitize(sanitizer: (String) -> String): AnnenErfaringDtoV2 = copy(
    rolle = rolle?.let { sanitizer(it) },
    beskrivelse = beskrivelse?.let { sanitizer(it) }
)

fun KursDtoV2.sanitize(sanitizer: (String) -> String): KursDtoV2 = copy(
    tittel = tittel?.let { sanitizer(it) },
    arrangor = arrangor?.let { sanitizer(it) }
)

fun SertifikatDtoV2.sanitize(sanitizer: (String) -> String): SertifikatDtoV2 = copy(
    tittel = tittel?.let { sanitizer(it) },
    utsteder = utsteder?.let { sanitizer(it) }
)

