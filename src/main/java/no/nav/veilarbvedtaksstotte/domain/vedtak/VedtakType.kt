package no.nav.veilarbvedtaksstotte.domain.vedtak

enum class VedtakType {
    // Vedtak opprettet under normale omstendigheter, dvs. av en veileder som en del av oppfølging.
    NORMAL_VEDTAK,
    // Ferdige vedtak som bestilles gjennom selvbetjeningsløsning for syntetiske testdata (f.eks. Dolly).
    TEST_VEDTAK
}
