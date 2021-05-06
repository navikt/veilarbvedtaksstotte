package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.kafka.VedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.UtkastetVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.VedtakEntity;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VedtakStatusEndringService {

    private final KafkaProducerService kafkaProducerService;

    private final MetricsService metricsService;

    private final VeilederService veilederService;

    private final VedtaksstotteRepository vedtaksstotteRepository;

    @Autowired
    public VedtakStatusEndringService(KafkaProducerService kafkaProducerService, MetricsService metricsService, VeilederService veilederService, VedtaksstotteRepository vedtaksstotteRepository) {
        this.kafkaProducerService = kafkaProducerService;
        this.metricsService = metricsService;
        this.veilederService = veilederService;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
    }

    public void utkastOpprettet(Vedtak vedtak) {
        Veileder veileder = veilederService.hentVeileder(vedtak.getVeilederIdent());

        KafkaVedtakStatusEndring.UtkastOpprettet utkastOpprettet = new KafkaVedtakStatusEndring.UtkastOpprettet()
                .setVeilederNavn(veileder.getNavn())
                .setVeilederIdent(vedtak.getVeilederIdent());

        setStatusEndringData(utkastOpprettet, vedtak);

        kafkaProducerService.sendVedtakStatusEndring(utkastOpprettet);
    }

    public void utkastSlettet(UtkastetVedtak utkastetVedtak) {
        kafkaProducerService.sendVedtakStatusEndring(
                lagVedtakStatusEndring(utkastetVedtak, VedtakStatusEndring.UTKAST_SLETTET)
        );

        metricsService.rapporterUtkastSlettet();
    }

    public void beslutterProsessStartet(UtkastetVedtak utkastetVedtak) {
        kafkaProducerService.sendVedtakStatusEndring(
                lagVedtakStatusEndring(utkastetVedtak, VedtakStatusEndring.BESLUTTER_PROSESS_STARTET)
        );
    }

    public void beslutterProsessAvbrutt(UtkastetVedtak utkastetVedtak) {
        kafkaProducerService.sendVedtakStatusEndring(
                lagVedtakStatusEndring(utkastetVedtak, VedtakStatusEndring.BESLUTTER_PROSESS_AVBRUTT)
        );
    }

    public void godkjentAvBeslutter(UtkastetVedtak utkastetVedtak) {
        kafkaProducerService.sendVedtakStatusEndring(
                lagVedtakStatusEndring(utkastetVedtak, VedtakStatusEndring.GODKJENT_AV_BESLUTTER)
        );
        metricsService.rapporterTidMellomUtkastOpprettetTilGodkjent(utkastetVedtak);
    }

    public void klarTilBeslutter(UtkastetVedtak utkastetVedtak) {
        kafkaProducerService.sendVedtakStatusEndring(
                lagVedtakStatusEndring(utkastetVedtak, VedtakStatusEndring.KLAR_TIL_BESLUTTER)
        );
    }

    public void klarTilVeileder(UtkastetVedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(
                lagVedtakStatusEndring(vedtak, VedtakStatusEndring.KLAR_TIL_VEILEDER)
        );
    }

    public void blittBeslutter(Vedtak vedtak, String beslutterIdent) {
        Veileder beslutter = veilederService.hentVeileder(beslutterIdent);

        KafkaVedtakStatusEndring.BliBeslutter bliBeslutter = new KafkaVedtakStatusEndring.BliBeslutter()
                .setBeslutterNavn(beslutter.getNavn())
                .setBeslutterIdent(beslutter.getIdent());

        setStatusEndringData(bliBeslutter, vedtak);

        kafkaProducerService.sendVedtakStatusEndring(bliBeslutter);
    }

    public void tattOverForBeslutter(Vedtak vedtak, String beslutterIdent) {
        Veileder beslutter = veilederService.hentVeileder(beslutterIdent);

        KafkaVedtakStatusEndring.OvertaForBeslutter overtaForBeslutter = new KafkaVedtakStatusEndring.OvertaForBeslutter()
                .setBeslutterNavn(beslutter.getNavn())
                .setBeslutterIdent(beslutter.getIdent());

        setStatusEndringData(overtaForBeslutter, vedtak);

        kafkaProducerService.sendVedtakStatusEndring(overtaForBeslutter);
    }

    public void tattOverForVeileder(Vedtak vedtak, String veilederIdent) {
        Veileder veileder = veilederService.hentVeileder(veilederIdent);

        KafkaVedtakStatusEndring.OvertaForVeileder overtaForVeileder = new KafkaVedtakStatusEndring.OvertaForVeileder()
                .setVeilederIdent(veileder.getIdent())
                .setVeilederNavn(veileder.getNavn());

        setStatusEndringData(overtaForVeileder, vedtak);

        kafkaProducerService.sendVedtakStatusEndring(overtaForVeileder);
    }

    public void vedtakSendt(Long vedtakId, String fnr) {
        UtkastetVedtak utkastetVedtak = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        KafkaVedtakStatusEndring.VedtakSendt statusEndring = new KafkaVedtakStatusEndring.VedtakSendt()
                .setInnsatsgruppe(utkastetVedtak.getInnsatsgruppe())
                .setHovedmal(utkastetVedtak.getHovedmal());

        setStatusEndringData(statusEndring, utkastetVedtak);

        kafkaProducerService.sendVedtakStatusEndring(statusEndring);
        kafkaProducerService.sendVedtakSendt(lagKafkaVedtakSendt(utkastetVedtak.getId()));

        metricsService.rapporterVedtakSendt(utkastetVedtak.getId());
        metricsService.rapporterTidFraRegistrering(utkastetVedtak, utkastetVedtak.getAktorId(), fnr);
        metricsService.rapporterVedtakSendtSykmeldtUtenArbeidsgiver(utkastetVedtak, fnr);
    }

    private KafkaVedtakSendt lagKafkaVedtakSendt(long vedtakId) {

        VedtakEntity vedtakEntity = vedtaksstotteRepository.hentVedtakEntity(vedtakId);

        return new KafkaVedtakSendt()
                .setId(vedtakEntity.getId())
                .setAktorId(vedtakEntity.getAktorId())
                .setHovedmal(vedtakEntity.getHovedmal())
                .setInnsatsgruppe(vedtakEntity.getInnsatsgruppe())
                .setVedtakSendt(vedtakEntity.getVedtakFattet())
                .setEnhetId(vedtakEntity.getOppfolgingsenhetId());
    }

    private KafkaVedtakStatusEndring lagVedtakStatusEndring(UtkastetVedtak vedtak, VedtakStatusEndring endring) {
        KafkaVedtakStatusEndring statusEndring = new KafkaVedtakStatusEndring();
        statusEndring.setVedtakStatusEndring(endring);
        setStatusEndringData(statusEndring, vedtak);
        return statusEndring;
    }

    private <T extends KafkaVedtakStatusEndring> void setStatusEndringData(T kafkaVedtakStatusEndring, Vedtak vedtak) {
        kafkaVedtakStatusEndring
            .setVedtakId(vedtak.getId())
            .setAktorId(vedtak.getAktorId())
            .setTimestamp(LocalDateTime.now());
    }

}
