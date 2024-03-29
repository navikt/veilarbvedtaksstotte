package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OyeblikksbildeRepositoryTest extends DatabaseTest {

    private final static String JSON_DATA = "{ \"data\": 42 }";
    private final static String JSON_DATA2 = "{ \"data\": 123 }";

    private static OyeblikksbildeRepository oyeblikksbildeRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeAll
    public static void setup() {
        oyeblikksbildeRepository = new OyeblikksbildeRepository(jdbcTemplate);
        vedtaksstotteRepository = new VedtaksstotteRepository(jdbcTemplate, transactor);
    }

    @BeforeEach
    public void cleanup() {
        DbTestUtils.cleanupDb(jdbcTemplate);
    }

    @Test
    public void testLagOyeblikksbildeFeilerHvisIkkeVedtakFinnes() {
        Oyeblikksbilde oyeblikksbilde = new Oyeblikksbilde(
                VEDTAK_ID_THAT_DOES_NOT_EXIST,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA
        );

        assertThrows(DataIntegrityViolationException.class, () -> oyeblikksbildeRepository.upsertOyeblikksbilde(oyeblikksbilde));
    }

    @Test
    public void testLagOgHentOyeblikksbilde() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        Oyeblikksbilde oyeblikksbilde = new Oyeblikksbilde(
                vedtakId,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA
        );

        oyeblikksbildeRepository.upsertOyeblikksbilde(oyeblikksbilde);

        List<Oyeblikksbilde> hentetOyeblikksbilder = oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);

        assertTrue(hentetOyeblikksbilder.size() > 0);
        assertEquals(oyeblikksbilde.getJson(), hentetOyeblikksbilder.get(0).getJson());
    }

    @Test
    public void testLagFlereOyblikksbildeMedSammeType() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        Oyeblikksbilde oyeblikksbildeRegInfoGammel = new Oyeblikksbilde(
                vedtakId,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA
        );

        Oyeblikksbilde oyeblikksbildeCV = new Oyeblikksbilde(
                vedtakId,
                OyeblikksbildeType.CV_OG_JOBBPROFIL,
                JSON_DATA
        );

        Oyeblikksbilde oyeblikksbildeRegInfoNy = new Oyeblikksbilde(
                vedtakId,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA2
        );

        List.of(oyeblikksbildeRegInfoGammel, oyeblikksbildeCV).forEach(oyeblikksbildeRepository::upsertOyeblikksbilde);
        oyeblikksbildeRepository.upsertOyeblikksbilde(oyeblikksbildeRegInfoNy);

        List<Oyeblikksbilde> hentetOyeblikksbilder = oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);

        assertEquals(2, hentetOyeblikksbilder.size());

        assertEquals(hentetOyeblikksbilder.stream()
                .filter(o -> o.getOyeblikksbildeType() == OyeblikksbildeType.REGISTRERINGSINFO)
                .map(Oyeblikksbilde::getJson)
                .findFirst().orElse(""), JSON_DATA2);

        assertEquals(hentetOyeblikksbilder.stream()
                .filter(o -> o.getOyeblikksbildeType() == OyeblikksbildeType.CV_OG_JOBBPROFIL)
                .map(Oyeblikksbilde::getJson)
                .findFirst().orElse(""), JSON_DATA);
    }

    @Test
    public void testSlettOyeblikksbilde() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        Oyeblikksbilde oyeblikksbilde1 = new Oyeblikksbilde(
                vedtakId,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA
        );

        Oyeblikksbilde oyeblikksbilde2 = new Oyeblikksbilde(
                vedtakId,
                OyeblikksbildeType.CV_OG_JOBBPROFIL,
                JSON_DATA
        );

        List<Oyeblikksbilde> oyeblikksbilder = List.of(oyeblikksbilde1, oyeblikksbilde2);
        oyeblikksbilder.forEach(oyeblikksbildeRepository::upsertOyeblikksbilde);

        oyeblikksbildeRepository.slettOyeblikksbilder(vedtakId);

        assertTrue(oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId).isEmpty());
    }

}
