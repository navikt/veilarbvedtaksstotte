package no.nav.veilarbvedtaksstotte.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.kafka.VedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Gjeldende14aVedtakKafkaDTOKt;
import no.nav.veilarbvedtaksstotte.domain.vedtak.HovedmalMedOkeDeltakelse;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VedtakHendelserService {

    private final KafkaProducerService kafkaProducerService;

    private final VeilederService veilederService;

    private final VedtaksstotteRepository vedtaksstotteRepository;

    public void utkastOpprettet(Vedtak vedtak) {
        Veileder veileder = veilederService.hentVeileder(vedtak.getVeilederIdent());

        KafkaVedtakStatusEndring.UtkastOpprettet utkastOpprettet = new KafkaVedtakStatusEndring.UtkastOpprettet()
                .setVeilederNavn(veileder.getNavn())
                .setVeilederIdent(vedtak.getVeilederIdent());

        setStatusEndringData(utkastOpprettet, vedtak);

        kafkaProducerService.sendVedtakStatusEndring(utkastOpprettet);
    }

    public void utkastSlettet(Vedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.UTKAST_SLETTET));
    }

    public void beslutterProsessStartet(Vedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.BESLUTTER_PROSESS_STARTET));
    }

    public void beslutterProsessAvbrutt(Vedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.BESLUTTER_PROSESS_AVBRUTT));
    }

    public void godkjentAvBeslutter(Vedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.GODKJENT_AV_BESLUTTER));
    }

    public void klarTilBeslutter(Vedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.KLAR_TIL_BESLUTTER));
    }

    public void klarTilVeileder(Vedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.KLAR_TIL_VEILEDER));
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

    public void vedtakSendt(Long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        KafkaVedtakStatusEndring.VedtakSendt statusEndring = new KafkaVedtakStatusEndring.VedtakSendt()
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setHovedmal(vedtak.getHovedmal());

        setStatusEndringData(statusEndring, vedtak);

        kafkaProducerService.sendVedtakStatusEndring(statusEndring);
        kafkaProducerService.sendVedtakSendt(lagKafkaVedtakSendt(vedtak));

        kafkaProducerService.sendSiste14aVedtak(
                new Siste14aVedtak(
                        AktorId.of(vedtak.getAktorId()),
                        vedtak.getInnsatsgruppe(),
                        HovedmalMedOkeDeltakelse.fraHovedmal(vedtak.getHovedmal()),
                        toZonedDateTime(vedtak.getVedtakFattet()),
                        false));

        kafkaProducerService.sendGjeldende14aVedtak(new AktorId(vedtak.getAktorId()), Gjeldende14aVedtakKafkaDTOKt.toGjeldende14aVedtakKafkaDTO(vedtak));
    }

    private KafkaVedtakSendt lagKafkaVedtakSendt(Vedtak vedtak) {
        return new KafkaVedtakSendt()
                .setId(vedtak.getId())
                .setAktorId(vedtak.getAktorId())
                .setHovedmal(vedtak.getHovedmal())
                .setInnsatsgruppe(vedtak.getInnsatsgruppe())
                .setVedtakSendt(vedtak.getVedtakFattet())
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
