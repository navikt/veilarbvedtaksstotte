package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.kafka.VedtakSendtTemplate;
import no.nav.veilarbvedtaksstotte.kafka.VedtakStatusEndringTemplate;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

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

    public void sendVedtakStatusEndring(KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        vedtakStatusEndringTemplate.send(kafkaVedtakStatusEndring);
    }

    public void sendTidligereFeiletMelding(FeiletKafkaMelding feiletKafkaMelding) {
        switch (feiletKafkaMelding.getTopic()) {
            case VEDTAK_SENDT:
                vedtakSendtTemplate.sendTidligereFeilet(feiletKafkaMelding);
                break;
            case VEDTAK_STATUS_ENDRING:
                vedtakStatusEndringTemplate.sendTidligereFeilet(feiletKafkaMelding);
                break;
        }
    }

}
