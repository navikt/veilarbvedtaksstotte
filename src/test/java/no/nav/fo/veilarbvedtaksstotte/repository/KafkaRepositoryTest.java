package no.nav.fo.veilarbvedtaksstotte.repository;

import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakSendt;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaVedtakStatusEndring;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.KafkaVedtakStatus;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.fo.veilarbvedtaksstotte.utils.SingletonePostgresContainer;
import org.junit.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

public class KafkaRepositoryTest {

    private static JdbcTemplate db;
    private static KafkaRepository kafkaRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonePostgresContainer.init().getDb();
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
                .setEnhetId(TEST_OPPFOLGINGSENHET_ID);

        assertThrows(DataIntegrityViolationException.class, () -> {
            kafkaRepository.lagreVedtakSendtKafkaFeil(vedtakSendt);
        });
    }

    @Test
    public void testLagOgHentVedtakSendtKafkaFeil() {

        vedtaksstotteRepository.opprettUtkast(
                TEST_AKTOR_ID,
                TEST_VEILEDER_IDENT,
                TEST_OPPFOLGINGSENHET_ID,
                TEST_OPPFOLGINGSENHET_NAVN
        );

        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
                .setId(vedtakId)
                .setVedtakSendt(LocalDateTime.now())
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                .setAktorId(TEST_AKTOR_ID)
                .setEnhetId(TEST_OPPFOLGINGSENHET_ID);

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
               TEST_OPPFOLGINGSENHET_ID,
               TEST_OPPFOLGINGSENHET_NAVN
        );

        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        KafkaVedtakSendt vedtakSendt = new KafkaVedtakSendt()
                .setId(vedtakId)
                .setVedtakSendt(LocalDateTime.now())
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                .setAktorId(TEST_AKTOR_ID)
                .setEnhetId(TEST_OPPFOLGINGSENHET_ID);

        kafkaRepository.lagreVedtakSendtKafkaFeil(vedtakSendt);
        kafkaRepository.slettVedtakSendtKafkaFeil(TEST_AKTOR_ID);

        assertTrue(kafkaRepository.hentFeiledeVedtakSendt().isEmpty());
    }

    @Test
    public void testLagOgHentVedtakStatusEndringKafkaFeil() {

        LocalDateTime now = LocalDateTime.now();
        KafkaVedtakStatus status = KafkaVedtakStatus.UTKAST_OPPRETTET;

        vedtaksstotteRepository.opprettUtkast(
                TEST_AKTOR_ID,
                TEST_VEILEDER_IDENT,
                TEST_OPPFOLGINGSENHET_ID,
                TEST_OPPFOLGINGSENHET_NAVN
        );

        KafkaVedtakStatusEndring vedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                .setAktorId(TEST_AKTOR_ID)
                .setStatusEndretTidspunkt(now)
                .setVedtakStatus(status);

        kafkaRepository.lagreVedtakStatusEndringKafkaFeil(vedtakStatusEndring);

        List<KafkaVedtakStatusEndring> feiledeVedtakStatusEndringer = kafkaRepository.hentFeiledeVedtakStatusEndringer();

        assertEquals(1, feiledeVedtakStatusEndringer.size());
        assertEquals(feiledeVedtakStatusEndringer.get(0).getAktorId(), vedtakStatusEndring.getAktorId());
        assertEquals(feiledeVedtakStatusEndringer.get(0).getVedtakStatus(), status);
    }

    @Test
    public void testVedtakStatusEndringKafkaFeilSlettes() {
        LocalDateTime now = LocalDateTime.now();
        KafkaVedtakStatus status = KafkaVedtakStatus.UTKAST_OPPRETTET;

        vedtaksstotteRepository.opprettUtkast(
                TEST_AKTOR_ID,
                TEST_VEILEDER_IDENT,
                TEST_OPPFOLGINGSENHET_ID,
                TEST_OPPFOLGINGSENHET_NAVN
        );

        KafkaVedtakStatusEndring vedtakStatusEndring = new KafkaVedtakStatusEndring()
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                .setAktorId(TEST_AKTOR_ID)
                .setStatusEndretTidspunkt(now)
                .setVedtakStatus(status);

        kafkaRepository.lagreVedtakStatusEndringKafkaFeil(vedtakStatusEndring);

        List<KafkaVedtakStatusEndring> feiledeVedtakStatusEndringer = kafkaRepository.hentFeiledeVedtakStatusEndringer();

        assertFalse(feiledeVedtakStatusEndringer.isEmpty());
        KafkaVedtakStatusEndring statusEndring = feiledeVedtakStatusEndringer.get(0);
        kafkaRepository.slettVedtakStatusEndringKafkaFeil(statusEndring.getVedtakId(), statusEndring.getVedtakStatus());

        assertTrue(kafkaRepository.hentFeiledeVedtakSendt().isEmpty());
    }

}
