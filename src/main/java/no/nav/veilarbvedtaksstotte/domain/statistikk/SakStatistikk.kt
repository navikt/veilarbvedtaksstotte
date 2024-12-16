package no.nav.veilarbvedtaksstotte.domain.statistikk

import no.nav.common.types.identer.AktorId
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*


data class SakStatistikk(
    val behandlingId: BigInteger,
    val behandlingUuid: UUID? = null,
    val relatertBehandlingId: BigInteger? = null,
    val relatertFagsystem: String? = null,
    val sakId: String? = null,
    val aktorId: String,
    val mottattTid: LocalDateTime,
    val registrertTid: LocalDateTime? = null,
    val ferdigbehandletTid: LocalDateTime? = null,
    val endretTid: LocalDateTime? = null,
    val tekniskTid: LocalDateTime? = null,
    val sakYtelse: String? = null,
    val behandlingType: String? = null,
    val behandlingStatus: String? = null,
    val behandlingResultat: String? = null,
    val behandlingMetode: String? = null,
    val opprettetAv: String? = null,
    val saksbehandler: String? = null,
    val ansvarligBeslutter: String? = null,
    val ansvarligEnhet: String? = null,
    val avsender: String? = null,
    val versjon: String? = null,
)
