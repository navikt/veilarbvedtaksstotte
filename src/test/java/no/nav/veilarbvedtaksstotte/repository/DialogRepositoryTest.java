package no.nav.veilarbvedtaksstotte.repository;

import no.nav.sbl.jdbc.Transactor;
import no.nav.veilarbvedtaksstotte.domain.DialogMelding;
import no.nav.veilarbvedtaksstotte.domain.enums.MeldingUnderType;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.util.List;

import static no.nav.veilarbvedtaksstotte.domain.enums.MeldingType.MANUELL;
import static no.nav.veilarbvedtaksstotte.domain.enums.MeldingType.SYSTEM;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

public class DialogRepositoryTest {

    private static JdbcTemplate db;
    private static DialogRepository dialogRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;
    private static Transactor transactor;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new Transactor(new DataSourceTransactionManager(db.getDataSource()));
        dialogRepository = new DialogRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, new KilderRepository(db), transactor);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void opprettDialogManuellMelding_skal_opprette_manuell_melding_i_dialog() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        dialogRepository.opprettDialogManuellMelding(vedtakId, TEST_VEILEDER_IDENT, TEST_DIALOG_MELDING);

        List<DialogMelding> meldinger = dialogRepository.hentDialogMeldinger(vedtakId);
        DialogMelding melding = meldinger.get(0);

        assertTrue(melding.getId() > 0);
        assertEquals(melding.getVedtakId(), vedtakId);
        assertEquals(melding.getOpprettetAvIdent(), TEST_VEILEDER_IDENT);
        assertEquals(melding.getMelding(), TEST_DIALOG_MELDING);
        assertNotNull(melding.getOpprettet());
        assertEquals(melding.getMeldingType(), MANUELL);
    }

    @Test
    public void opprettDialogSystemMelding_skal_opprette_system_melding_i_dialog() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        dialogRepository.opprettDialogSystemMelding(vedtakId, TEST_VEILEDER_IDENT, MeldingUnderType.UTKAST_OPPRETTET);

        List<DialogMelding> meldinger = dialogRepository.hentDialogMeldinger(vedtakId);
        DialogMelding melding = meldinger.get(0);

        assertTrue(melding.getId() > 0);
        assertEquals(melding.getVedtakId(), vedtakId);
        assertEquals(melding.getOpprettetAvIdent(), TEST_VEILEDER_IDENT);
        assertEquals(melding.getMeldingUnderType(), MeldingUnderType.UTKAST_OPPRETTET);
        assertNotNull(melding.getOpprettet());
        assertEquals(melding.getMeldingType(), SYSTEM);
    }

    @Test
    public void slettDialogMeldinger_skal_slette_alle_meldinger_i_dialog() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();
        dialogRepository.opprettDialogSystemMelding(vedtakId, TEST_VEILEDER_IDENT, MeldingUnderType.UTKAST_OPPRETTET);
        dialogRepository.opprettDialogManuellMelding(vedtakId, TEST_VEILEDER_IDENT, TEST_DIALOG_MELDING);

        dialogRepository.slettDialogMeldinger(vedtakId);

        List<DialogMelding> meldinger = dialogRepository.hentDialogMeldinger(vedtakId);

        assertTrue(meldinger.isEmpty());
    }


}
