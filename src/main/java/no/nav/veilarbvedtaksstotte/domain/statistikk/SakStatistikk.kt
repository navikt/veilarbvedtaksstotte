package no.nav.veilarbvedtaksstotte.domain.statistikk

import no.nav.common.types.identer.EnhetId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import java.math.BigInteger
import java.time.Instant
import java.util.*

const val SAK_YTELSE = "ARBEIDSRETTET_OPPFOLGINGSBEHOV"

data class SakStatistikk(
    val behandlingId: BigInteger,
    val aktorId: String,
    val relatertBehandlingId: BigInteger? = null,
    val relatertFagsystem: Fagsystem? = null,
    val sakId: String? = null,
    val mottattTid: Instant,
    val registrertTid: Instant,
    val ferdigbehandletTid: Instant? = null,
    val endretTid: Instant,
    val tekniskTid: Instant? = null,
    val sakYtelse: String? = null,
    val behandlingType: BehandlingType,
    val behandlingStatus: BehandlingStatus,
    val behandlingResultat: BehandlingResultat? = null,
    val behandlingMetode: BehandlingMetode,
    val opprettetAv: String,
    val saksbehandler: String,
    val ansvarligBeslutter: String? = null,
    val ansvarligEnhet: EnhetId? = null,
    val avsender: Fagsystem = Fagsystem.OPPFOLGINGSVEDTAK_14A,
    val versjon: String? = null,
    val oppfolgingPeriodeUUID: UUID,
    val innsatsgruppe: BehandlingResultat? = null,
    val hovedmal: HovedmalNy? = null
)

enum class BehandlingType {
    FORSTEGANGSBEHANDLING,
    REVURDERING
}

enum class BehandlingStatus {
    UNDER_BEHANDLING,
    SENDT_TIL_KVALITETSSIKRING,
    FATTET,
    AVBRUTT
}

enum class BehandlingResultat {
    GODE_MULIGHETER,
    TRENGER_VEILEDNING,
    TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
    JOBBE_DELVIS,
    LITEN_MULIGHET_TIL_A_JOBBE
}

fun Innsatsgruppe.toBehandlingResultat(): BehandlingResultat {
    return when (this) {
        Innsatsgruppe.STANDARD_INNSATS -> BehandlingResultat.GODE_MULIGHETER
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS -> BehandlingResultat.TRENGER_VEILEDNING
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS -> BehandlingResultat.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE
        Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS -> BehandlingResultat.JOBBE_DELVIS
        Innsatsgruppe.VARIG_TILPASSET_INNSATS -> BehandlingResultat.LITEN_MULIGHET_TIL_A_JOBBE
    }
}

enum class BehandlingMetode {
    MANUELL,
    TOTRINNS
}

enum class Fagsystem {
    ARENA,
    OPPFOLGINGSVEDTAK_14A
}

enum class HovedmalNy {
    SKAFFE_ARBEID,
    BEHOLDE_ARBEID
}
