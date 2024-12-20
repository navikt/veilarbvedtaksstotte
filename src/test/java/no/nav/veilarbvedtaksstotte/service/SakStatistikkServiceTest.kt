package no.nav.veilarbvedtaksstotte.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.reset
import org.mockito.MockitoAnnotations

class SakStatistikkServiceTest   {

    companion object {
        @Mock
        lateinit var sakStatistikkService: SakStatistikkService
    }

    @BeforeEach
    fun before() {
        MockitoAnnotations.openMocks(this)
        reset(sakStatistikkService)
    }
    @Test
    fun `SakStatistikkService test nummer en`() {

        Assertions.assertEquals("Test1", "Test1")
    }
}