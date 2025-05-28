package no.nav.veilarbvedtaksstotte.service

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.InsertAllResponse
import no.nav.common.job.leader_election.LeaderElectionClient
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
        private var leaderElectionClient: LeaderElectionClient = mock(LeaderElectionClient::class.java)

        @JvmStatic
        @BeforeAll
        fun setupOnce() {
            bigQueryService = BigQueryService("test-dataset", "test-table", bigQuery)
            sakStatistikkRepository = SakStatistikkRepository(jdbcTemplate)
            sakStatistikkResendingService = SakStatistikkResendingService(
                sakStatistikkRepository!!,
                bigQueryService!!,
                leaderElectionClient
            )
        }

    }

    @BeforeEach
    fun setup() {
        DbTestUtils.cleanupDb(jdbcTemplate)
        whenever(bigQuery.insertAll(any())).thenReturn(mock(InsertAllResponse::class.java))
        whenever(leaderElectionClient.isLeader).thenReturn(true)
    }

    @Test
    fun `test resending, mottatt_tid være lik registrert_tid når REVURDERING`() {
        // Arrange
        val behandlingId1 = 3001.toBigInteger()
        val behandlingId2 = 3002.toBigInteger()
        val mottattTid1 = Instant.now().plus(2, ChronoUnit.DAYS)
        val registrertTid1 = Instant.now().minus(1, ChronoUnit.DAYS)
        val mottattTid2 = Instant.now().plus(1, ChronoUnit.DAYS)
        val registrertTid2 = Instant.now()
        val statistikkrad1 = lagStatistikkRad(behandlingId1, mottattTid1,registrertTid1)
        val statistikkrad2 = lagStatistikkRad(behandlingId2, mottattTid2, registrertTid2)
        sakStatistikkRepository!!.insertSakStatistikkRadBatch(listOf(statistikkrad1, statistikkrad2))

        // Act
        sakStatistikkResendingService!!.resendStatistikk()

        // Assert
        val lagretStatistikkRadAlt1 = sakStatistikkRepository!!.hentSakStatistikkListeAlt(behandlingId1)
        val lagretStatistikkRadAlt2 = sakStatistikkRepository!!.hentSakStatistikkListeAlt(behandlingId2)
        assert(lagretStatistikkRadAlt1[0].mottattTid == mottattTid1)
        assert(lagretStatistikkRadAlt1[1].mottattTid == registrertTid1)
        assert(lagretStatistikkRadAlt2[0].mottattTid == mottattTid2)
        assert(lagretStatistikkRadAlt2[1].mottattTid == registrertTid2)
    }

    private fun lagStatistikkRad(behandlingId: BigInteger, mottattTid: Instant, registrertTid: Instant): SakStatistikk {
        return SakStatistikk(
            aktorId = AktorId.of("2004140973848"),
            oppfolgingPeriodeUUID = UUID.fromString("1a930d0d-6931-403e-852c-b85e39673aaf"),
            behandlingId = behandlingId,
            relatertBehandlingId = 3000.toBigInteger(),
            relatertFagsystem = null,
            sakId = "Arbeidsoppfølging",
            mottattTid = mottattTid,
            registrertTid = registrertTid,
            ferdigbehandletTid = Instant.now(),
            endretTid = Instant.now(),
            sakYtelse = SAK_YTELSE,
            behandlingType = BehandlingType.REVURDERING,
            behandlingStatus = BehandlingStatus.AVBRUTT,
            behandlingResultat = BehandlingResultat.GODE_MULIGHETER,
            behandlingMetode = BehandlingMetode.MANUELL,
            innsatsgruppe = BehandlingResultat.GODE_MULIGHETER,
            hovedmal = HovedmalNy.BEHOLDE_ARBEID,
            opprettetAv = "Z123456",
            saksbehandler = "Z123456",
            ansvarligBeslutter = "Z123456",
            ansvarligEnhet = EnhetId.of("0220")
        )
    }
}


