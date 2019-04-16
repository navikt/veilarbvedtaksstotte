package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.schedule.VedtakSendtKafkaFeilSchedule;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import({ VedtakSendtKafkaFeilSchedule.class })
public class ScheduleConfig {}
