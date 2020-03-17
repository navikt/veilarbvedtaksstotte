package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.DialogMelding;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

public class DialogRepositoryTest {

    private static JdbcTemplate db;
    private static DialogRepository dialogRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        dialogRepository = new DialogRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, new KilderRepository(db));
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void opprettDialogMelding_skal_opprette_dialog_melding() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        dialogRepository.opprettDialogMelding(vedtakId, TEST_VEILEDER_IDENT, TEST_DIALOG_MELDING);

        List<DialogMelding> meldinger = dialogRepository.hentDialogMeldinger(vedtakId);
        DialogMelding melding = meldinger.get(0);

        assertTrue(melding.getId() > 0);
        assertEquals(melding.getVedtakId(), vedtakId);
        assertEquals(melding.getOpprettetAvIdent(), TEST_VEILEDER_IDENT);
        assertEquals(melding.getMelding(), TEST_DIALOG_MELDING);
        assertNotNull(melding.getOpprettet());
    }

    @Test
    public void slettDialogMeldinger_skal_slette_dialog_melding() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        dialogRepository.opprettDialogMelding(vedtakId, TEST_VEILEDER_IDENT, TEST_DIALOG_MELDING);

        dialogRepository.slettDialogMeldinger(vedtakId);

        List<DialogMelding> meldinger = dialogRepository.hentDialogMeldinger(vedtakId);

        assertTrue(meldinger.isEmpty());
    }


}
