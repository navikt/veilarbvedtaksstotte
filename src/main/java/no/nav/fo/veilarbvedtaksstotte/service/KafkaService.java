package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakSendtTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;

@Service
public class KafkaService {

    private VedtakSendtTemplate vedtakSendtTemplate;

    @Inject
    public KafkaService(VedtakSendtTemplate vedtakSendtTemplate) {
        this.vedtakSendtTemplate = vedtakSendtTemplate;
    }

    public void sendTestVedtak() {
        System.out.println("SEND VEDTAK");

        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
            .setAktorId("1284181123913")
            .setInnsatsgruppe(Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS)
            .setVedtakSendt(LocalDateTime.now());

        vedtakSendtTemplate.send(vedtakSendt);
    }

}
