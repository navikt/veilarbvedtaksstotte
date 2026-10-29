package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt

data class BrukereMedAntall(
    val brukere: List<BeslutteroversiktBruker>,
    val totaltAntall: Long,
)
