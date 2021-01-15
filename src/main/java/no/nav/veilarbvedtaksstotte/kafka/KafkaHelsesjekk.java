package no.nav.veilarbvedtaksstotte.kafka;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.veilarbvedtaksstotte.repository.KafkaRepository;
import no.nav.veilarbvedtaksstotte.repository.domain.MeldingType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Slf4j
@Component
public class KafkaHelsesjekk implements MeterBinder, HealthCheck {

    private final KafkaRepository kafkaRepository;

    @Autowired
    public KafkaHelsesjekk(KafkaRepository kafkaRepository) {
        this.kafkaRepository = kafkaRepository;
    }

    @Override
    public HealthCheckResult checkHealth() {
        int totaltFeiledeMeldinger = hentFeiledeKafkaMeldinger(MeldingType.CONSUMED)
                + hentFeiledeKafkaMeldinger(MeldingType.PRODUCED);

        if (totaltFeiledeMeldinger > 0) {
            return HealthCheckResult.unhealthy(format("Det ligger lagret %d kafka meldinger som ikke er konsumert/publisert", totaltFeiledeMeldinger));
        }

        return HealthCheckResult.healthy();
    }

    @Override
    public void bindTo(@NotNull MeterRegistry meterRegistry) {
        Gauge.builder("kafka_failed_consumed_messages",
                this, value -> value.hentFeiledeKafkaMeldinger(MeldingType.CONSUMED))
                .description("Antall feilede kafka meldinger ikke har blitt konsumert")
                .register(meterRegistry);

        Gauge.builder("kafka_failed_produced_messages",
                this, value -> value.hentFeiledeKafkaMeldinger(MeldingType.PRODUCED))
                .description("Antall feilede kafka meldinger som ikke har blitt publisert")
                .register(meterRegistry);
    }

    private int hentFeiledeKafkaMeldinger(MeldingType meldingType) {
        return kafkaRepository.hentFeiledeKafkaMeldinger(meldingType).size();
    }

}


