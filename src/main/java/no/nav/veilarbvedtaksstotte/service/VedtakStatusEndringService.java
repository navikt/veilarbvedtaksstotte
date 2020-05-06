package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import no.nav.veilarbvedtaksstotte.domain.enums.VedtakStatusEndring;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;

@Service
public class VedtakStatusEndringService {

    private final KafkaService kafkaService;

    private final MetricsService metricsService;

    private final VeilederService veilederService;

    @Inject
    public VedtakStatusEndringService(KafkaService kafkaService, MetricsService metricsService, VeilederService veilederService) {
        this.kafkaService = kafkaService;
        this.metricsService = metricsService;
        this.veilederService = veilederService;
    }

    public void utkastOpprettet(Vedtak vedtak) {
        kafkaService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.UTKAST_OPPRETTET));
    }

    public void utkastSlettet(Vedtak vedtak) {
        kafkaService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.UTKAST_SLETTET));
        metricsService.rapporterUtkastSlettet();
    }

    public void beslutterProsessStartet(Vedtak vedtak) {
        kafkaService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.BESLUTTER_PROSESS_STARTET));
    }

    public void godkjentAvBeslutter(Vedtak vedtak) {
        kafkaService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.GODKJENT_AV_BESLUTTER));
        metricsService.rapporterTidMellomUtkastOpprettetTilGodkjent(vedtak);
    }

    public void klarTilBeslutter(Vedtak vedtak) {
        kafkaService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.KLAR_TIL_BESLUTTER));
    }

    public void klarTilVeileder(Vedtak vedtak) {
        kafkaService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.KLAR_TIL_VEILEDER));
    }

    public void blittBeslutter(Vedtak vedtak, String beslutterIdent) {
        Veileder beslutter = veilederService.hentVeileder(beslutterIdent);

        KafkaVedtakStatusEndring.BliBeslutter bliBeslutter = new KafkaVedtakStatusEndring.BliBeslutter()
                .setBeslutterNavn(beslutter.getNavn())
                .setBeslutterIdent(beslutter.getIdent());

        setStatusEndringData(bliBeslutter, vedtak);

        kafkaService.sendVedtakStatusEndring(bliBeslutter);
    }

    public void tattOverForBeslutter(Vedtak vedtak, String beslutterIdent) {
        Veileder beslutter = veilederService.hentVeileder(beslutterIdent);

        KafkaVedtakStatusEndring.OvertaForBeslutter overtaForBeslutter = new KafkaVedtakStatusEndring.OvertaForBeslutter()
                .setBeslutterNavn(beslutter.getNavn())
                .setBeslutterIdent(beslutter.getIdent());

        setStatusEndringData(overtaForBeslutter, vedtak);

        kafkaService.sendVedtakStatusEndring(overtaForBeslutter);
    }

    public void tattOverForVeileder(Vedtak vedtak, String veilederIdent) {
        Veileder veileder = veilederService.hentVeileder(veilederIdent);

        KafkaVedtakStatusEndring.OvertaForVeileder overtaForVeileder = new KafkaVedtakStatusEndring.OvertaForVeileder()
                .setVeilederIdent(veileder.getIdent())
                .setVeilederNavn(veileder.getNavn());

        setStatusEndringData(overtaForVeileder, vedtak);

        kafkaService.sendVedtakStatusEndring(overtaForVeileder);
    }

    public void vedtakSendt(Vedtak vedtak, String fnr) {
        KafkaVedtakStatusEndring.VedtakSendt statusEndring = new KafkaVedtakStatusEndring.VedtakSendt()
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setHovedmal(vedtak.getHovedmal());

        setStatusEndringData(statusEndring, vedtak);

        kafkaService.sendVedtakStatusEndring(statusEndring);
        kafkaService.sendVedtakSendt(lagKafkaVedtakSendt(vedtak));

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
