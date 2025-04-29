package no.nav.veilarbvedtaksstotte.domain.statistikk

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import java.math.BigInteger
import java.time.Instant
import java.util.*

const val SAK_YTELSE = "ARBEIDSRETTET_OPPFOLGINGSBEHOV"

data class SakStatistikk(
    val sekvensnummer: Long? = null,
    val behandlingId: BigInteger? = null,
    val aktorId: AktorId? = null,
    val relatertBehandlingId: BigInteger? = null,
    val relatertFagsystem: Fagsystem? = null,
    val sakId: String? = null,
    val mottattTid: Instant? = null,
    val registrertTid: Instant? = null,
    val ferdigbehandletTid: Instant? = null,
    val endretTid: Instant? = null,
    val sakYtelse: String? = null,
    val behandlingType: BehandlingType? = null,
    val behandlingStatus: BehandlingStatus? = null,
    val behandlingResultat: BehandlingResultat? = null,
    val behandlingMetode: BehandlingMetode? = null,
    val opprettetAv: String? = null,
    val saksbehandler: String? = null,
    val ansvarligBeslutter: String? = null,
    val ansvarligEnhet: EnhetId? = null,
    val fagsystemNavn: Fagsystem = Fagsystem.OPPFOLGINGSVEDTAK_14A,
    val fagsystemVersjon: String? = null,
    val oppfolgingPeriodeUUID: UUID? = null,
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
    KVALITETSSIKRING_GODKJENT,
    FATTET,
    AVBRUTT,
    AVSLUTTET
}

enum class BehandlingResultat {
    GODE_MULIGHETER,
    TRENGER_VEILEDNING,
    TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
    JOBBE_DELVIS,
    LITEN_MULIGHET_TIL_A_JOBBE,
    AVBRUTT
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
    AUTOMATISK,
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


fun SakStatistikk.validate() {
    if (behandlingId == null) {
        throw IllegalStateException("behandlingId kan ikke være null")
    }
    if (aktorId == null) {
        throw IllegalStateException("aktorId kan ikke være null")
    }
    if (mottattTid == null) {
        throw IllegalStateException("mottattTid kan ikke være null")
    }
    if (registrertTid == null) {
        throw IllegalStateException("registrertTid kan ikke være null")
    }
    if (endretTid == null) {
        throw IllegalStateException("endretTid kan ikke være null")
    }
    if (sakYtelse == null) {
        throw IllegalStateException("sakYtelse kan ikke være null")
    }
    if (behandlingType == null) {
        throw IllegalStateException("behandlingType kan ikke være null")
    }
    if (behandlingStatus == null) {
        throw IllegalStateException("behandlingStatus kan ikke være null")
    }
    if (behandlingMetode == null) {
        throw IllegalStateException("behandlingMetode kan ikke være null")
    }
    if (opprettetAv == null) {
        throw IllegalStateException("opprettetAv kan ikke være null")
    }
    if (saksbehandler == null) {
        throw IllegalStateException("saksbehandler kan ikke være null")
    }
    if (ansvarligEnhet == null) {
        throw IllegalStateException("ansvarligEnhet kan ikke være null")
    }
    if (fagsystemVersjon == null) {
        throw IllegalStateException("versjon kan ikke være null")
    }
    if (oppfolgingPeriodeUUID == null) {
        throw IllegalStateException("oppfolgingPeriodeUUID kan ikke være null")
    }
}