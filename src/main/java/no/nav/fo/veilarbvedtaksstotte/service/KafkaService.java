package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakSendtTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.sql.Timestamp;

@Service
public class KafkaService {

    private VedtakSendtTemplate vedtakSendtTemplate;

    @Inject
    public KafkaService(VedtakSendtTemplate vedtakSendtTemplate) {
        this.vedtakSendtTemplate = vedtakSendtTemplate;
    }

    public void sendVedtak(Vedtak vedtak, String aktorId) {
        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
                .setAktorId(aktorId)
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setVedtakSendt(new Timestamp(System.currentTimeMillis()));

        vedtakSendtTemplate.send(vedtakSendt);
    }

}
