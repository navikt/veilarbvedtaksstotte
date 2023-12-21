package no.nav.veilarbvedtaksstotte.client.registrering.dto

import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.BesvarelseSvar
import java.time.LocalDateTime

data class RegistreringsdataDto(
    val opprettetDato: LocalDateTime?,
    var besvarelse: BesvarelseSvar? = null,
    var teksterForBesvarelse: List<TekstForSporsmal> = emptyList(),
    val sisteStilling: Stilling?,
    val profilering: Profilering? = null,
    var manueltRegistrertAv: Veileder? = null,

    var endretAv: String?,
    var endretTidspunkt: LocalDateTime?
) {
    data class TekstForSporsmal(
        val sporsmalId: String,
        val sporsmal: String,
        var svar: String,
    )

    data class Stilling(
        val label: String? = null,
        val konseptId: Long = 0,
        val styrk08: String? = null,
    )

    data class Profilering(
        var innsatsgruppe: ProfilertInnsatsgruppe? = null,
        var alder: Int?,
        var jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean?,
    )

    enum class ProfilertInnsatsgruppe {
        STANDARD_INNSATS,
        SITUASJONSBESTEMT_INNSATS,
        BEHOV_FOR_ARBEIDSEVNEVURDERING
    }

    data class NavEnhet(val id: String, val navn: String)

    data class Veileder(var ident: String? = null, var enhet: NavEnhet? = null)
}