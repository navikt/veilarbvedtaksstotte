package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.dto.OyeblikksbildeInputDto;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static no.nav.veilarbvedtaksstotte.utils.TestUtils.readTestResourceFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OyeblikksbildeRepositoryTest extends DatabaseTest {

    private final static String JSON_DATA = getCvData();
    private final static String JSON_DATA2 = getRegistreringData();

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
        OyeblikksbildeInputDto oyeblikksbilde = new OyeblikksbildeInputDto(
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

        OyeblikksbildeInputDto oyeblikksbilde = new OyeblikksbildeInputDto(
                vedtakId,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA
        );

        oyeblikksbildeRepository.upsertOyeblikksbilde(oyeblikksbilde);

        List<Oyeblikksbilde> hentetOyeblikksbilder = oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);

        assertTrue(!hentetOyeblikksbilder.isEmpty());
        assertEquals(oyeblikksbilde.getJson(), hentetOyeblikksbilder.get(0).getJson());
    }

    @Test
    public void testLagFlereOyblikksbildeMedSammeType() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        OyeblikksbildeInputDto oyeblikksbildeRegInfoGammel = new OyeblikksbildeInputDto(
                vedtakId,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA
        );

        OyeblikksbildeInputDto oyeblikksbildeCV = new OyeblikksbildeInputDto(
                vedtakId,
                OyeblikksbildeType.CV_OG_JOBBPROFIL,
                JSON_DATA
        );

        OyeblikksbildeInputDto oyeblikksbildeRegInfoNy = new OyeblikksbildeInputDto(
                vedtakId,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA2
        );

        List.of(oyeblikksbildeRegInfoGammel, oyeblikksbildeCV).forEach(oyeblikksbildeRepository::upsertOyeblikksbilde);
        oyeblikksbildeRepository.upsertOyeblikksbilde(oyeblikksbildeRegInfoNy);

        List<Oyeblikksbilde> hentetOyeblikksbilder = oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);

        Assertions.assertEquals(2, hentetOyeblikksbilder.size());

        Assertions.assertEquals(hentetOyeblikksbilder.stream()
                .filter(o -> o.getOyeblikksbildeType() == OyeblikksbildeType.REGISTRERINGSINFO)
                .map(Oyeblikksbilde::getJson)
                .findFirst().orElse(""), JSON_DATA2);

        Assertions.assertEquals(hentetOyeblikksbilder.stream()
                .filter(o -> o.getOyeblikksbildeType() == OyeblikksbildeType.CV_OG_JOBBPROFIL)
                .map(Oyeblikksbilde::getJson)
                .findFirst().orElse(""), JSON_DATA);
    }

    @Test
    public void testSlettOyeblikksbilde() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        OyeblikksbildeInputDto oyeblikksbilde1 = new OyeblikksbildeInputDto(
                vedtakId,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA
        );

        OyeblikksbildeInputDto oyeblikksbilde2 = new OyeblikksbildeInputDto(
                vedtakId,
                OyeblikksbildeType.CV_OG_JOBBPROFIL,
                JSON_DATA
        );

        List<OyeblikksbildeInputDto> oyeblikksbilder = List.of(oyeblikksbilde1, oyeblikksbilde2);
        oyeblikksbilder.forEach(oyeblikksbildeRepository::upsertOyeblikksbilde);

        oyeblikksbildeRepository.slettOyeblikksbilder(vedtakId);

        Assertions.assertTrue(oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId).isEmpty());
    }

    private static String getCvData() {
        return readTestResourceFile("oyeblikksbilde-cv.json");
    }

    private static String getRegistreringData() {
        return readTestResourceFile("registrering.json");
    }

}
