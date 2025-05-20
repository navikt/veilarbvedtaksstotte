package no.nav.veilarbvedtaksstotte.service

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.InsertAllResponse
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingMetode
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingResultat
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingStatus
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingType
import no.nav.veilarbvedtaksstotte.domain.statistikk.HovedmalNy
import no.nav.veilarbvedtaksstotte.domain.statistikk.SAK_YTELSE
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class SakStatistikkResendingServiceTest : DatabaseTest() {

    companion object {
        private var bigQueryService: BigQueryService? = null
        private var sakStatistikkRepository: SakStatistikkRepository? = null
        private var sakStatistikkResendingService: SakStatistikkResendingService? = null
        private var bigQuery: BigQuery = mock(BigQuery::class.java)

        @JvmStatic
        @BeforeAll
        fun setupOnce() {
            bigQueryService = BigQueryService("test-dataset", "test-table", bigQuery)
            sakStatistikkRepository = SakStatistikkRepository(jdbcTemplate)
            sakStatistikkResendingService = SakStatistikkResendingService(sakStatistikkRepository!!, bigQueryService!!)
        }

        @BeforeEach
        fun setup() {
            DbTestUtils.cleanupDb(jdbcTemplate)
            whenever(bigQuery.insertAll(any())).thenReturn(mock(InsertAllResponse::class.java))
        }

    }

    @Test
    fun `test resending, behandling_status AVBRUTT gir nye rader med behandling_status AVSLUTTET`() {
        // Arrange
        val behandlingId1 = 3001.toBigInteger()
        val behandlingId2 = 3002.toBigInteger()
        val statistikkrad1 = lagStatistikkRad(behandlingId1)
        val statistikkrad2 = lagStatistikkRad(behandlingId2)
        sakStatistikkRepository!!.insertSakStatistikkRadBatch(listOf(statistikkrad1, statistikkrad2))

        // Act
        sakStatistikkResendingService!!.resendStatistikk()

        // Assert
        val lagretStatistikkRadAlt1 = sakStatistikkRepository!!.hentSakStatistikkListeAlt(behandlingId1)
        val lagretStatistikkRadAlt2 = sakStatistikkRepository!!.hentSakStatistikkListeAlt(behandlingId2)
        assert(lagretStatistikkRadAlt1[0].behandlingStatus?.name == "AVBRUTT")
        assert(lagretStatistikkRadAlt1[1].behandlingStatus?.name == "AVSLUTTET")
        assert(lagretStatistikkRadAlt2[0].behandlingStatus?.name == "AVBRUTT")
        assert(lagretStatistikkRadAlt2[1].behandlingStatus?.name == "AVSLUTTET")
    }

    private fun lagStatistikkRad(behandlingId: BigInteger): SakStatistikk {
        return SakStatistikk(
            aktorId = AktorId.of("2004140973848"),
            oppfolgingPeriodeUUID = UUID.fromString("1a930d0d-6931-403e-852c-b85e39673aaf"),
            behandlingId = behandlingId,
            relatertBehandlingId = 3000.toBigInteger(),
            relatertFagsystem = null,
            sakId = "Arbeidsoppfølging",
            mottattTid = Instant.now().minus(2, ChronoUnit.DAYS),
            registrertTid = Instant.now().minus(1, ChronoUnit.DAYS),
            ferdigbehandletTid = Instant.now(),
            endretTid = Instant.now(),
            sakYtelse = SAK_YTELSE,
            behandlingType = BehandlingType.FORSTEGANGSBEHANDLING,
            behandlingStatus = BehandlingStatus.AVBRUTT,
            behandlingResultat = BehandlingResultat.GODE_MULIGHETER,
            behandlingMetode = BehandlingMetode.MANUELL,
            innsatsgruppe = BehandlingResultat.GODE_MULIGHETER,
            hovedmal = HovedmalNy.BEHOLDE_ARBEID,
            opprettetAv = "Z123456",
            saksbehandler = "Z123456",
            ansvarligBeslutter = "Z123456",
            ansvarligEnhet = EnhetId.of("0220"))
    }
}


