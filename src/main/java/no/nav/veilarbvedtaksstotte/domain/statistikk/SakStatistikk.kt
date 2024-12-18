package no.nav.veilarbvedtaksstotte.domain.statistikk

import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*


data class SakStatistikk(
    val behandlingId: BigInteger,
    val aktorId: String,
    val oppfolgingPeriodeUUID: UUID? = null,
    val behandlingUuid: UUID? = null,
    val relatertBehandlingId: BigInteger? = null,
    val relatertFagsystem: String? = null,
    val sakId: String? = null,
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
    val innsatsgruppe: String? = null,
    val hovedmal: String? = null,
    val opprettetAv: String? = null,
    val saksbehandler: String? = null,
    val ansvarligBeslutter: String? = null,
    val ansvarligEnhet: String? = null,
    val avsender: String? = null,
    val versjon: String? = null,
)
