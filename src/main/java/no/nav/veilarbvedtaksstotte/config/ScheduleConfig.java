package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.schedule.KafkaFeilSchedule;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ KafkaFeilSchedule.class })
public class ScheduleConfig {}
