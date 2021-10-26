package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvh;
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvhHovedmalKode;
import no.nav.pto_schema.kafka.avro.Vedtak14aFattetDvhInnsatsgruppeKode;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak.HovedmalMedOkeDeltakelse;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.kafka.VedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static no.nav.veilarbvedtaksstotte.utils.TimeUtils.toInstant;
import static no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime;

@Service
public class VedtakStatusEndringService {

    private final KafkaProducerService kafkaProducerService;

    private final MetricsService metricsService;

    private final VeilederService veilederService;

    private final VedtaksstotteRepository vedtaksstotteRepository;


    @Autowired
    public VedtakStatusEndringService(
            KafkaProducerService kafkaProducerService,
            MetricsService metricsService,
            VeilederService veilederService,
            VedtaksstotteRepository vedtaksstotteRepository
    ) {
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

    public void utkastSlettet(Vedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.UTKAST_SLETTET));
        metricsService.rapporterUtkastSlettet();
    }

    public void beslutterProsessStartet(Vedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.BESLUTTER_PROSESS_STARTET));
    }

    public void beslutterProsessAvbrutt(Vedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.BESLUTTER_PROSESS_AVBRUTT));
    }

    public void godkjentAvBeslutter(Vedtak vedtak) {
        kafkaProducerService.sendVedtakStatusEndring(lagVedtakStatusEndring(vedtak, VedtakStatusEndring.GODKJENT_AV_BESLUTTER));
        metricsService.rapporterTidMellomUtkastOpprettetTilGodkjent(vedtak);
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

    public void vedtakSendt(Long vedtakId, Fnr fnr) {
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

        kafkaProducerService.sendVedtakFattetDvh(
                Vedtak14aFattetDvh.newBuilder()
                        .setId(vedtakId)
                        .setAktorId(vedtak.getAktorId())
                        .setHovedmalKode(mapHovedmalTilAvroType(vedtak.getHovedmal()))
                        .setInnsatsgruppeKode(mapInnsatsgruppeTilAvroType(vedtak.getInnsatsgruppe()))
                        .setVedtakFattet(toInstant(vedtak.getVedtakFattet()))
                        .setOppfolgingsenhetId(vedtak.getOppfolgingsenhetId())
                        .setVeilederIdent(vedtak.getVeilederIdent())
                        .setBeslutterIdent(vedtak.getBeslutterIdent())
                        .build()
        );

        metricsService.rapporterVedtakSendt(vedtak);
        metricsService.rapporterTidFraRegistrering(vedtak, vedtak.getAktorId(), fnr.get());
        metricsService.rapporterVedtakSendtSykmeldtUtenArbeidsgiver(vedtak, fnr.get());
    }

    private static Vedtak14aFattetDvhHovedmalKode mapHovedmalTilAvroType(Hovedmal hovedmal) {
        if (hovedmal == null) {
            return null;
        }

        switch (hovedmal) {
            case SKAFFE_ARBEID:
                return Vedtak14aFattetDvhHovedmalKode.SKAFFE_ARBEID;
            case BEHOLDE_ARBEID:
                return Vedtak14aFattetDvhHovedmalKode.BEHOLDE_ARBEID;
        }

        throw new IllegalStateException("Manglende mapping av hovedmål");
    }

    private static Vedtak14aFattetDvhInnsatsgruppeKode mapInnsatsgruppeTilAvroType(Innsatsgruppe innsatsgruppe) {
        if (innsatsgruppe == null) {
            return null;
        }

        switch (innsatsgruppe) {
            case STANDARD_INNSATS:
                return Vedtak14aFattetDvhInnsatsgruppeKode.STANDARD_INNSATS;
            case SITUASJONSBESTEMT_INNSATS:
                return Vedtak14aFattetDvhInnsatsgruppeKode.SITUASJONSBESTEMT_INNSATS;
            case SPESIELT_TILPASSET_INNSATS:
                return Vedtak14aFattetDvhInnsatsgruppeKode.SPESIELT_TILPASSET_INNSATS;
            case GRADERT_VARIG_TILPASSET_INNSATS:
                return Vedtak14aFattetDvhInnsatsgruppeKode.GRADERT_VARIG_TILPASSET_INNSATS;
            case VARIG_TILPASSET_INNSATS:
                return Vedtak14aFattetDvhInnsatsgruppeKode.VARIG_TILPASSET_INNSATS;
        }

        throw new IllegalStateException("Manglende mapping av innsatsgruppe");
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
