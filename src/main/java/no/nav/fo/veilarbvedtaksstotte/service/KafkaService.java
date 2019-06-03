package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.kafka.VedtakSendtTemplate;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class KafkaService {

    private VedtakSendtTemplate vedtakSendtTemplate;
    private VedtaksstotteRepository vedtaksstotteRepository;

    @Inject
    public KafkaService(VedtakSendtTemplate vedtakSendtTemplate,
                        VedtaksstotteRepository vedtaksstotteRepository) {
        this.vedtakSendtTemplate = vedtakSendtTemplate;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
    }

    public void sendVedtak(long vedtakId) {

        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);

        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
                .setId(vedtak.getId())
                .setAktorId(vedtak.getAktorId())
                .setHovedmaal(vedtak.getHovedmal())
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setVedtakSendt(vedtak.getSistOppdatert())
                .setEnhetId(vedtak.getVeilederEnhetId());

        vedtakSendtTemplate.send(vedtakSendt);
    }

    public void sendTidligereFeiletVedtak(KafkaVedtakSendt kafkaVedtakSendt) {
        vedtakSendtTemplate.sendTidligereFeilet(kafkaVedtakSendt);
    }

}
