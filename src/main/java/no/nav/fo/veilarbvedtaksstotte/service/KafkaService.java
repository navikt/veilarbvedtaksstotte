package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakStatus;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.KafkaVedtakStatusType;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakSendtTemplate;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakStatusTemplate;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;

@Service
public class KafkaService {

    private VedtakSendtTemplate vedtakSendtTemplate;

    private VedtakStatusTemplate vedtakStatusTemplate;

    private VedtaksstotteRepository vedtaksstotteRepository;

    @Inject
    public KafkaService(VedtakSendtTemplate vedtakSendtTemplate,
                        VedtakStatusTemplate vedtakStatusTemplate,
                        VedtaksstotteRepository vedtaksstotteRepository) {
        this.vedtakSendtTemplate = vedtakSendtTemplate;
        this.vedtakStatusTemplate = vedtakStatusTemplate;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
    }

    public void sendVedtak(long vedtakId) {

        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);

        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
                .setId(vedtak.getId())
                .setAktorId(vedtak.getAktorId())
                .setHovedmal(vedtak.getHovedmal())
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setVedtakSendt(vedtak.getSistOppdatert())
                .setEnhetId(vedtak.getVeilederEnhetId());

        vedtakSendtTemplate.send(vedtakSendt);
    }

    public void sendTidligereFeiletVedtak(KafkaVedtakSendt kafkaVedtakSendt) {
        vedtakSendtTemplate.sendTidligereFeilet(kafkaVedtakSendt);
    }

    // TODO: Litt teit å sende med fnr. Enten drop eller brukt aktor id service?
    public void sendVedtakStatus(Vedtak vedtak, String fnr, KafkaVedtakStatusType statusType) {

        KafkaVedtakStatus vedtakStatus = new KafkaVedtakStatus()
                .setId(vedtak.getId())
                .setBrukerAktorId(vedtak.getAktorId())
                .setBrukerFnr(fnr)
                .setHovedmal(vedtak.getHovedmal())
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setVedtakStatus(statusType)
                .setStatusEndretTidspunkt(LocalDateTime.now())
                .setSistRedigertTidspunkt(vedtak.getSistOppdatert());

        vedtakStatusTemplate.send(vedtakStatus);
    }

    public void sendTidligereFeiletVedtakStatus(KafkaVedtakStatus kafkaVedtakStatus) {
        vedtakStatusTemplate.sendTidligereFeilet(kafkaVedtakStatus);
    }

}
