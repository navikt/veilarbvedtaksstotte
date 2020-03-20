package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaVedtakStatus;
import no.nav.veilarbvedtaksstotte.kafka.VedtakSendtTemplate;
import no.nav.veilarbvedtaksstotte.kafka.VedtakStatusEndringTemplate;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;

@Service
public class KafkaService {

    private VedtakSendtTemplate vedtakSendtTemplate;

    private VedtakStatusEndringTemplate vedtakStatusEndringTemplate;

    private VedtaksstotteRepository vedtaksstotteRepository;

    @Inject
    public KafkaService(VedtakSendtTemplate vedtakSendtTemplate,
                        VedtakStatusEndringTemplate vedtakStatusEndringTemplate,
                        VedtaksstotteRepository vedtaksstotteRepository) {
        this.vedtakSendtTemplate = vedtakSendtTemplate;
        this.vedtakStatusEndringTemplate = vedtakStatusEndringTemplate;
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
                .setEnhetId(vedtak.getOppfolgingsenhetId());

        vedtakSendtTemplate.send(vedtakSendt);
    }

    public void sendTidligereFeiletVedtak(KafkaVedtakSendt kafkaVedtakSendt) {
        vedtakSendtTemplate.sendTidligereFeilet(kafkaVedtakSendt);
    }

    public void sendVedtakStatusEndring(Vedtak vedtak, KafkaVedtakStatus status) {
        KafkaVedtakStatusEndring vedtakStatus = new KafkaVedtakStatusEndring()
                .setVedtakId(vedtak.getId())
                .setAktorId(vedtak.getAktorId())
                .setHovedmal(vedtak.getHovedmal())
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setStatusEndretTidspunkt(LocalDateTime.now())
                .setVedtakStatus(status);

        vedtakStatusEndringTemplate.send(vedtakStatus);
    }

    public void sendTidligereFeiletVedtakStatusEndring(KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        vedtakStatusEndringTemplate.sendTidligereFeilet(kafkaVedtakStatusEndring);
    }

}