package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.client.person.OpplysningerOmArbeidssoekerMedProfilering;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvErrorStatus;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvInnhold;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import no.nav.veilarbvedtaksstotte.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestData.VEDTAK_ID_THAT_DOES_NOT_EXIST;
import static no.nav.veilarbvedtaksstotte.utils.TestUtils.readTestResourceFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class OyeblikksbildeRepositoryTest extends DatabaseTest {

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
        CvDto cvDto = new CvDto.CvMedError(CvErrorStatus.IKKE_DELT);

        assertThrowsExactly(DataIntegrityViolationException.class, () -> oyeblikksbildeRepository.upsertCVOyeblikksbilde(VEDTAK_ID_THAT_DOES_NOT_EXIST, cvDto));
    }

    @Test
    public void testLagOgHentOyeblikksbilde() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        String cvJobbprofilJson = TestUtils.readTestResourceFile("testdata/cv-jobbprofil.json");
        CvInnhold cvInnhold = JsonUtils.fromJson(cvJobbprofilJson, CvInnhold.class);

        oyeblikksbildeRepository.upsertCVOyeblikksbilde(vedtakId, new CvDto.CVMedInnhold(cvInnhold));

        List<OyeblikksbildeDto> hentetOyeblikksbilder = oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);

        Assertions.assertFalse(hentetOyeblikksbilder.isEmpty());

        CvInnhold cvInnholdFraDB = JsonUtils.fromJson(hentetOyeblikksbilder.get(0).getJson(), CvInnhold.class);
        assertEquals(cvInnhold, cvInnholdFraDB);
    }

    @Test
    public void testOppdateringOyblikksbilde() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        OpplysningerOmArbeidssoekerMedProfilering opplysningerOmArbeidssoekerMedProfileringResponseDto = JsonUtils.fromJson(getOpplysningerOmArbeidssoekerMedProfileringData(), OpplysningerOmArbeidssoekerMedProfilering.class);
        oyeblikksbildeRepository.upsertArbeidssokerRegistretOyeblikksbilde(vedtakId, opplysningerOmArbeidssoekerMedProfileringResponseDto);
        oyeblikksbildeRepository.upsertCVOyeblikksbilde(vedtakId, new CvDto.CvMedError(CvErrorStatus.IKKE_DELT));

        List<OyeblikksbildeDto> hentetOyeblikksbilderFørOppdatering = oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);

        assertEquals(2, hentetOyeblikksbilderFørOppdatering.size());

        String cvJobbprofilJson = TestUtils.readTestResourceFile("testdata/cv-jobbprofil.json");
        CvInnhold cvInnhold = JsonUtils.fromJson(cvJobbprofilJson, CvInnhold.class);
        oyeblikksbildeRepository.upsertCVOyeblikksbilde(vedtakId, new CvDto.CVMedInnhold(cvInnhold));


        List<OyeblikksbildeDto> hentetOyeblikksbilderEtterOppdatering = oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);

        String jsonOpplysningerOmArbeidssoekerMedProfileringFraDb = hentetOyeblikksbilderEtterOppdatering.stream()
                .filter(o -> o.getOyeblikksbildeType() == OyeblikksbildeType.ARBEIDSSOKERREGISTRET)
                .map(OyeblikksbildeDto::getJson)
                .findFirst().orElse("");
        OpplysningerOmArbeidssoekerMedProfilering opplysningerOmArbeidssoekerMedProfileringResponseDB = JsonUtils.fromJson(jsonOpplysningerOmArbeidssoekerMedProfileringFraDb, OpplysningerOmArbeidssoekerMedProfilering.class);
        assertEquals(opplysningerOmArbeidssoekerMedProfileringResponseDB, opplysningerOmArbeidssoekerMedProfileringResponseDto);


        String jsonCVFraDB = hentetOyeblikksbilderEtterOppdatering.stream()
                .filter(o -> o.getOyeblikksbildeType() == OyeblikksbildeType.CV_OG_JOBBPROFIL)
                .map(OyeblikksbildeDto::getJson)
                .findFirst().orElse("");
        CvInnhold cvMedInnholdFraDB = JsonUtils.fromJson(jsonCVFraDB, CvInnhold.class);
        assertEquals(cvMedInnholdFraDB, cvInnhold);
    }

    @Test
    public void testSlettOyeblikksbilde() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();
        oyeblikksbildeRepository.upsertCVOyeblikksbilde(vedtakId, new CvDto.CvMedError(CvErrorStatus.IKKE_DELT));

        oyeblikksbildeRepository.slettOyeblikksbilder(vedtakId);

        Assertions.assertTrue(oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId).isEmpty());
    }

    private static String getOpplysningerOmArbeidssoekerMedProfileringData() {
        return readTestResourceFile("testdata/opplysningerOmArbeidssoekerMedProfilering.json");
    }


}
