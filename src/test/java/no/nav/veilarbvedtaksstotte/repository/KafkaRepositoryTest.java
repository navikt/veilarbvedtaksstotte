package no.nav.veilarbvedtaksstotte.repository;

import no.nav.sbl.jdbc.Transactor;
import no.nav.veilarbvedtaksstotte.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaTopic;
import no.nav.veilarbvedtaksstotte.domain.enums.VedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KafkaRepositoryTest {

    private static JdbcTemplate db;
    private static Transactor transactor;
    private static KafkaRepository kafkaRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new Transactor(new DataSourceTransactionManager(db.getDataSource()));
        kafkaRepository = new KafkaRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, new KilderRepository(db), new DialogRepository(db), transactor);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void testLagOgHentVedtakSendtKafkaFeil() {

        vedtaksstotteRepository.opprettUtkast(
                TEST_AKTOR_ID,
                TEST_VEILEDER_IDENT,
                TEST_OPPFOLGINGSENHET_ID
        );

        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
                .setId(vedtakId)
                .setVedtakSendt(LocalDateTime.now())
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                .setAktorId(TEST_AKTOR_ID)
                .setEnhetId(TEST_OPPFOLGINGSENHET_ID);

        String jsonPayload = toJson(vedtakSendt);
        kafkaRepository.lagreFeiletKafkaMelding(KafkaTopic.VEDTAK_SENDT, vedtakSendt.getAktorId(), jsonPayload);

        List<FeiletKafkaMelding> feiledeVedtakSendt = kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopic.VEDTAK_SENDT);

        assertEquals(1, feiledeVedtakSendt.size());
        assertEquals(feiledeVedtakSendt.get(0).getKey(), vedtakSendt.getAktorId());
        assertEquals(feiledeVedtakSendt.get(0).getJsonPayload(), jsonPayload);
    }

    @Test
    public void testVedtakSendtKafkaFeilSlettes() {
       vedtaksstotteRepository.opprettUtkast(
                TEST_AKTOR_ID,
                TEST_VEILEDER_IDENT,
               TEST_OPPFOLGINGSENHET_ID
        );

        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
                .setId(vedtakId)
                .setVedtakSendt(LocalDateTime.now())
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                .setAktorId(TEST_AKTOR_ID)
                .setEnhetId(TEST_OPPFOLGINGSENHET_ID);

        kafkaRepository.lagreFeiletKafkaMelding(KafkaTopic.VEDTAK_SENDT, vedtakSendt.getAktorId(), toJson(vedtakSendt));
        List<FeiletKafkaMelding> feiletKafkaMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopic.VEDTAK_SENDT);
        kafkaRepository.slettFeiletKafkaMelding(feiletKafkaMeldinger.get(0).getId());

        assertTrue(kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopic.VEDTAK_SENDT).isEmpty());
    }

    @Test
    public void testLagOgHentVedtakStatusEndringKafkaFeil() {

        LocalDateTime now = LocalDateTime.now();
        VedtakStatusEndring status = VedtakStatusEndring.UTKAST_OPPRETTET;

        vedtaksstotteRepository.opprettUtkast(
                TEST_AKTOR_ID,
                TEST_VEILEDER_IDENT,
                TEST_OPPFOLGINGSENHET_ID
        );

        KafkaVedtakStatusEndring vedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setAktorId(TEST_AKTOR_ID)
                .setTimestamp(now)
                .setVedtakStatusEndring(status);

        String jsonPayload = toJson(vedtakStatusEndring);
        kafkaRepository.lagreFeiletKafkaMelding(KafkaTopic.VEDTAK_STATUS_ENDRING, vedtakStatusEndring.getAktorId(), jsonPayload);

        List<FeiletKafkaMelding> feiledeVedtakStatusEndringer = kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopic.VEDTAK_STATUS_ENDRING);

        assertEquals(1, feiledeVedtakStatusEndringer.size());
        assertEquals(feiledeVedtakStatusEndringer.get(0).getKey(), vedtakStatusEndring.getAktorId());
        assertEquals(feiledeVedtakStatusEndringer.get(0).getJsonPayload(), jsonPayload);
    }

    @Test
    public void testVedtakStatusEndringKafkaFeilSlettes() {
        LocalDateTime now = LocalDateTime.now();
        VedtakStatusEndring status = VedtakStatusEndring.UTKAST_OPPRETTET;

        vedtaksstotteRepository.opprettUtkast(
                TEST_AKTOR_ID,
                TEST_VEILEDER_IDENT,
                TEST_OPPFOLGINGSENHET_ID
        );

        KafkaVedtakStatusEndring vedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setAktorId(TEST_AKTOR_ID)
                .setTimestamp(now)
                .setVedtakStatusEndring(status);

        kafkaRepository.lagreFeiletKafkaMelding(KafkaTopic.VEDTAK_STATUS_ENDRING, vedtakStatusEndring.getAktorId(), toJson(vedtakStatusEndring));

        List<FeiletKafkaMelding> feiledeVedtakStatusEndringer = kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopic.VEDTAK_STATUS_ENDRING);
        kafkaRepository.slettFeiletKafkaMelding(feiledeVedtakStatusEndringer.get(0).getId());
        assertTrue(kafkaRepository.hentFeiledeKafkaMeldinger(KafkaTopic.VEDTAK_STATUS_ENDRING).isEmpty());
    }

}
