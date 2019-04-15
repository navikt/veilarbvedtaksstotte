package no.nav.fo.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import org.springframework.kafka.core.KafkaTemplate;

import static no.nav.json.JsonUtils.toJson;

@Slf4j
public class VedtakSendtTemplate {

    private final String topic;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public VedtakSendtTemplate(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void send(KafkaVedtakSendt vedtakSendt) {
        final String serialisertBruker = toJson(vedtakSendt);

        System.out.println("SENDER " + serialisertBruker);

        kafkaTemplate.send(topic, vedtakSendt.getAktorId(), serialisertBruker)
                .addCallback(
                    sendResult -> onSuccess(vedtakSendt),
                    throwable -> onError(throwable, vedtakSendt)
                );
    }

    private void onSuccess(KafkaVedtakSendt vedtakSendt) {
        System.out.println("ON SUCCESS " + vedtakSendt.toString());
        log.info("KafkaVedtakSendt: " + vedtakSendt.toString());
        // oppfolgingsbrukerEndringRepository.deleteFeiletBruker(user);
        //log.info("Bruker med aktorid {} har lagt på kafka", user.getAktoerid().get());
    }

    private void onError(Throwable throwable, KafkaVedtakSendt vedtakSendt) {
        System.out.println("ON ERROR " + vedtakSendt.toString());
        log.error("KafkaVedtakSendt: Kunne ikke publisere melding til kafka-topic", throwable);
        //log.info("Forsøker å insertere feilede bruker med aktorid {} i FEILEDE_KAFKA_BRUKERE", user.getAktoerid());
        //oppfolgingsbrukerEndringRepository.insertFeiletBruker(user);
    }

}
