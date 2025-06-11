package no.nav.veilarbvedtaksstotte.client.veilarboppfolging.request

import no.nav.common.types.identer.Fnr

class StartOppfolgingDto(
    val fnr: Fnr,
    val henviserSystem: String,
)

data class RegistrerIkkeArbeidssokerDto(
    val resultat: String,
    var kode: ArenaRegistreringResultat
)
enum class ArenaRegistreringResultat {
    OK_REGISTRERT_I_ARENA,
    FNR_FINNES_IKKE,
    KAN_REAKTIVERES_FORENKLET,
    BRUKER_ALLEREDE_ARBS,
    BRUKER_ALLEREDE_IARBS,
    UKJENT_FEIL
}
