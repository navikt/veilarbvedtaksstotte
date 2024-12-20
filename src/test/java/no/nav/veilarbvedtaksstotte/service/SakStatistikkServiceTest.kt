package no.nav.veilarbvedtaksstotte.service

import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.reset

class SakStatistikkServiceTest   {

    companion object {
        lateinit var sakStatistikkService: SakStatistikkService
    }

    @BeforeEach
    fun before() {
        reset(sakStatistikkService)
    }
}