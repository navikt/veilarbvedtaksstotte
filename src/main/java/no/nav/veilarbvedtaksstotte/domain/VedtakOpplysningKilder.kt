package no.nav.veilarbvedtaksstotte.domain

enum class VedtakOpplysningKilder(val desc: String) {
    CV("CV-en"),
    REGISTRERING("Svarene dine fra da du registrerte deg"),
    EGENVURDERING("Svarene dine om behov for veiledning"),
    ARBEIDSSOKERREGISTERET("Det du fortalte oss da du ble registrert som arbeidssøker"),
    CV_NN("CV-en/jobbønska din(e) på nav.no"),
    EGENVURDERING_NN("Svara dine om behov for rettleiing"),
    ARBEIDSSOKERREGISTERET_NN("Det du fortalde oss da du vart registrert som arbeidssøkar"),
}