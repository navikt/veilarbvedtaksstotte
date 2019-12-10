package no.nav.fo.veilarbvedtaksstotte.repository;

import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

@Ignore
public class KafkaRepositoryTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static JdbcTemplate db;
    private static KafkaRepository kafkaRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = DbTestUtils.setupDb(pg.getEmbeddedPostgres());
        KilderRepository kilderRepository = new KilderRepository(db);
        kafkaRepository = new KafkaRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void testLagVedtakSendtKafkaFeilSkalFeileHvisVedtakIkkeLaget() {
        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
                .setId(VEDTAK_ID_THAT_DOES_NOT_EXIST)
                .setVedtakSendt(LocalDateTime.now())
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                .setAktorId(TEST_AKTOR_ID)
                .setEnhetId(TEST_VEILEDER_ENHET_ID);

        assertThrows(DataIntegrityViolationException.class, () -> {
            kafkaRepository.lagreVedtakSendtKafkaFeil(vedtakSendt);
        });
    }

    @Test
    public void testLagOgHentVedtakSendtKafkaFeil() {

        vedtaksstotteRepository.opprettUtkast(
                TEST_AKTOR_ID,
                TEST_VEILEDER_IDENT,
                TEST_VEILEDER_ENHET_ID,
                TEST_VEILEDER_ENHET_NAVN
        );

        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
                .setId(vedtakId)
                .setVedtakSendt(LocalDateTime.now())
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                .setAktorId(TEST_AKTOR_ID)
                .setEnhetId(TEST_VEILEDER_ENHET_ID);

        kafkaRepository.lagreVedtakSendtKafkaFeil(vedtakSendt);

        List<KafkaVedtakSendt> feiledeVedtakSendt = kafkaRepository.hentFeiledeVedtakSendt();

        assertEquals(1, feiledeVedtakSendt.size());
        assertEquals(feiledeVedtakSendt.get(0).getAktorId(), vedtakSendt.getAktorId());
    }

    @Test
    public void testVedtakSendtKafkaFeilSlettes() {
       vedtaksstotteRepository.opprettUtkast(
                TEST_AKTOR_ID,
                TEST_VEILEDER_IDENT,
                TEST_VEILEDER_ENHET_ID,
                TEST_VEILEDER_ENHET_NAVN
        );

        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
                .setId(vedtakId)
                .setVedtakSendt(LocalDateTime.now())
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                .setAktorId(TEST_AKTOR_ID)
                .setEnhetId(TEST_VEILEDER_ENHET_ID);

        kafkaRepository.lagreVedtakSendtKafkaFeil(vedtakSendt);
        kafkaRepository.slettVedtakSendtKafkaFeil(TEST_AKTOR_ID);

        assertTrue(kafkaRepository.hentFeiledeVedtakSendt().isEmpty());
    }

}
