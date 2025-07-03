package no.nav.veilarbvedtaksstotte.config

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MicrometerAnnotationConfiguration {
    @Bean
    fun timedAspect(registry: MeterRegistry): TimedAspect {
        return TimedAspect(registry)
    }

    @Bean
    fun antallTegnFjernetVedVask(meterRegistry: MeterRegistry): DistributionSummary {
        return DistributionSummary.builder("antall_ugyldige_tegn_fjernet")
            .description("Antall ugyldige tegn fjernet fra pdf input")
            .register(meterRegistry)
    }

}
