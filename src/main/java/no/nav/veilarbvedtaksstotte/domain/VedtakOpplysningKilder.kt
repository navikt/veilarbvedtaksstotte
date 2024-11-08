package no.nav.veilarbvedtaksstotte.domain

enum class VedtakOpplysningKilder(val desc: String) {
    CV("CV-en"),
    REGISTRERING("Svarene dine fra da du registrerte deg"),
    EGENVURDERING("Svarene dine om behov for veiledning"),
    ARBEIDSSOKERREGISTERET("Det du fortalte oss da du ble registrert som arbeidssøker"),
}