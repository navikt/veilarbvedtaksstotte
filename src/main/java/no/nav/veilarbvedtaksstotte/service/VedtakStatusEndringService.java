package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov.HovedmalMedOkeDeltakelse;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.kafka.KafkaProducer;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.kafka.dto.VedtakStatusEndring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VedtakStatusEndringService {

    private final KafkaProducer kafkaProducer;

    private final MetricsService metricsService;

    private final VeilederService veilederService;

    @Autowired
    public VedtakStatusEndringService(KafkaProducer kafkaProducer,
                                      MetricsService metricsService,
                                      VeilederService veilederService) {
        this.kafkaProducer = kafkaProducer;
        this.metricsService = metricsService;
        this.veilederService = veilederService;
    }

    public void utkastOpprettet(Vedtak vedtak) {
        Veileder veileder = veilederService.hentVeileder(vedtak.getVeilederIdent());

        KafkaVedtakStatusEndring.UtkastOpprettet utkastOpprettet = new KafkaVedtakStatusEndring.UtkastOpprettet()
                .setVeilederNavn(veileder.getNavn())
                .setVeilederIdent(vedtak.getVeilederIdent());

        setStatusEndringData(utkastOpprettet, vedtak);

        kafkaProducer.sendVedtakStatusEndring(utkastOpprettet);
    }

    public void utkastSlettet(Vedtak vedtak) {
        kafkaProducer.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.UTKAST_SLETTET));
        metricsService.rapporterUtkastSlettet();
    }

    public void beslutterProsessStartet(Vedtak vedtak) {
        kafkaProducer.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.BESLUTTER_PROSESS_STARTET));
    }

    public void beslutterProsessAvbrutt(Vedtak vedtak) {
        kafkaProducer.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.BESLUTTER_PROSESS_AVBRUTT));
    }

    public void godkjentAvBeslutter(Vedtak vedtak) {
        kafkaProducer.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.GODKJENT_AV_BESLUTTER));
        metricsService.rapporterTidMellomUtkastOpprettetTilGodkjent(vedtak);
    }

    public void klarTilBeslutter(Vedtak vedtak) {
        kafkaProducer.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.KLAR_TIL_BESLUTTER));
    }

    public void klarTilVeileder(Vedtak vedtak) {
        kafkaProducer.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.KLAR_TIL_VEILEDER));
    }

    public void blittBeslutter(Vedtak vedtak, String beslutterIdent) {
        Veileder beslutter = veilederService.hentVeileder(beslutterIdent);

        KafkaVedtakStatusEndring.BliBeslutter bliBeslutter = new KafkaVedtakStatusEndring.BliBeslutter()
                .setBeslutterNavn(beslutter.getNavn())
                .setBeslutterIdent(beslutter.getIdent());

        setStatusEndringData(bliBeslutter, vedtak);

        kafkaProducer.sendVedtakStatusEndring(bliBeslutter);
    }

    public void tattOverForBeslutter(Vedtak vedtak, String beslutterIdent) {
        Veileder beslutter = veilederService.hentVeileder(beslutterIdent);

        KafkaVedtakStatusEndring.OvertaForBeslutter overtaForBeslutter = new KafkaVedtakStatusEndring.OvertaForBeslutter()
                .setBeslutterNavn(beslutter.getNavn())
                .setBeslutterIdent(beslutter.getIdent());

        setStatusEndringData(overtaForBeslutter, vedtak);

        kafkaProducer.sendVedtakStatusEndring(overtaForBeslutter);
    }

    public void tattOverForVeileder(Vedtak vedtak, String veilederIdent) {
        Veileder veileder = veilederService.hentVeileder(veilederIdent);

        KafkaVedtakStatusEndring.OvertaForVeileder overtaForVeileder = new KafkaVedtakStatusEndring.OvertaForVeileder()
                .setVeilederIdent(veileder.getIdent())
                .setVeilederNavn(veileder.getNavn());

        setStatusEndringData(overtaForVeileder, vedtak);

        kafkaProducer.sendVedtakStatusEndring(overtaForVeileder);
    }

    public void vedtakSendt(Vedtak vedtak, String fnr) {
        KafkaVedtakStatusEndring.VedtakSendt statusEndring = new KafkaVedtakStatusEndring.VedtakSendt()
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setHovedmal(vedtak.getHovedmal());

        setStatusEndringData(statusEndring, vedtak);

        kafkaProducer.sendVedtakStatusEndring(statusEndring);
        kafkaProducer.sendVedtakSendt(lagKafkaVedtakSendt(vedtak));

        kafkaProducer.sendInnsatsbehov(
                new Innsatsbehov(
                        AktorId.of(vedtak.getAktorId()),
                        vedtak.getInnsatsgruppe(),
                        HovedmalMedOkeDeltakelse.fraHovedmal(vedtak.getHovedmal())));

        metricsService.rapporterVedtakSendt(vedtak);
        metricsService.rapporterTidFraRegistrering(vedtak, vedtak.getAktorId(), fnr);
        metricsService.rapporterVedtakSendtSykmeldtUtenArbeidsgiver(vedtak, fnr);
    }

    private KafkaVedtakSendt lagKafkaVedtakSendt(Vedtak vedtak) {
        return new KafkaVedtakSendt()
                .setId(vedtak.getId())
                .setAktorId(vedtak.getAktorId())
                .setHovedmal(vedtak.getHovedmal())
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setVedtakSendt(vedtak.getSistOppdatert())
                .setEnhetId(vedtak.getOppfolgingsenhetId());
    }

    private KafkaVedtakStatusEndring lagVedtakStatusEndring(Vedtak vedtak, VedtakStatusEndring endring) {
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
