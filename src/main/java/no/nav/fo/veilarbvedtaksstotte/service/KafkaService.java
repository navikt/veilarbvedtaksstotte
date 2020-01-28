package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakStatusEndring;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.KafkaVedtakStatus;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.VedtakStatus;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakSendtTemplate;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakStatusEndringTemplate;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
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
                .setEnhetId(vedtak.getVeilederEnhetId());

        vedtakSendtTemplate.send(vedtakSendt);
    }

    public void sendTidligereFeiletVedtak(KafkaVedtakSendt kafkaVedtakSendt) {
        vedtakSendtTemplate.sendTidligereFeilet(kafkaVedtakSendt);
    }

    public void sendVedtakStatusEndring(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);

        KafkaVedtakStatusEndring vedtakStatus = new KafkaVedtakStatusEndring()
                .setId(vedtakId)
                .setAktorId(vedtak.getAktorId())
                .setHovedmal(vedtak.getHovedmal())
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setSistRedigertTidspunkt(vedtak.getSistOppdatert())
                .setStatusEndretTidspunkt(LocalDateTime.now())
                .setVedtakStatus(utledVedtakStatus(vedtak));

        vedtakStatusEndringTemplate.send(vedtakStatus);
    }

    public void sendTidligereFeiletVedtakStatusEndring(KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        vedtakStatusEndringTemplate.sendTidligereFeilet(kafkaVedtakStatusEndring);
    }

    private KafkaVedtakStatus utledVedtakStatus(Vedtak vedtak) {
        if (vedtak.isSendtTilBeslutter()) {
            return KafkaVedtakStatus.SENDT_TIL_BESLUTTER;
        } else if (vedtak.getVedtakStatus() == VedtakStatus.SENDT) {
            return KafkaVedtakStatus.SENDT_TIL_BRUKER;
        } else {
            return KafkaVedtakStatus.UTKAST_OPPRETTET;
        }
    }

}
