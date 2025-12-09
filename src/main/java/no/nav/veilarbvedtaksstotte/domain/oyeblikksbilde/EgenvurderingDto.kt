package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde

import java.time.LocalDateTime
import java.util.UUID

data class EgenvurderingDto(
    val sistOppdatert: String? = null,
    val svar: List<Svar>? = null,
) : EgenvurderingData {
    data class Svar(
        val spm: String,
        val svar: String? = null,
        val oppfolging: String? = null,
        val dialogId: String? = null,
    )
}

data class EgenvurderingV2Dto(
    val egenvurderingId: UUID,
    val sendtInnTidspunkt: LocalDateTime,
    val dialogId: Long?,
    val sporsmal: String = "Hva slags veiledning ønsker du?",
    val svar: String,
) : EgenvurderingData
