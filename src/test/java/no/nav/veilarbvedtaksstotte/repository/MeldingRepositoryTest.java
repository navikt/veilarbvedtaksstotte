package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.dialog.DialogMelding;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMelding;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

public class MeldingRepositoryTest {

    private static JdbcTemplate db;
    private static MeldingRepository meldingRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;
    private static TransactionTemplate transactor;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        meldingRepository = new MeldingRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, transactor);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void opprettDialogMelding_skal_opprette_dialog_melding() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        meldingRepository.opprettDialogMelding(vedtakId, TEST_VEILEDER_IDENT, TEST_DIALOG_MELDING);

        List<DialogMelding> meldinger = meldingRepository.hentDialogMeldinger(vedtakId);
        DialogMelding melding = meldinger.get(0);

        assertTrue(melding.getId() > 0);
        assertEquals(melding.getVedtakId(), vedtakId);
        assertEquals(melding.getOpprettetAvIdent(), TEST_VEILEDER_IDENT);
        assertEquals(melding.getMelding(), TEST_DIALOG_MELDING);
        assertNotNull(melding.getOpprettet());
    }

    @Test
    public void opprettSystemMelding_skal_opprette_system_melding() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        meldingRepository.opprettSystemMelding(vedtakId, SystemMeldingType.UTKAST_OPPRETTET, TEST_VEILEDER_IDENT);

        List<SystemMelding> meldinger = meldingRepository.hentSystemMeldinger(vedtakId);
        SystemMelding melding = meldinger.get(0);

        assertTrue(melding.getId() > 0);
        assertEquals(melding.getVedtakId(), vedtakId);
        assertEquals(melding.getSystemMeldingType(), SystemMeldingType.UTKAST_OPPRETTET);
        assertEquals(melding.getUtfortAvIdent(), TEST_VEILEDER_IDENT);
        assertNotNull(melding.getOpprettet());
    }

    @Test
    public void slettMeldinger_skal_slette_alle_meldinger() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        meldingRepository.opprettDialogMelding(vedtakId, TEST_VEILEDER_IDENT, TEST_DIALOG_MELDING);

        meldingRepository.slettMeldinger(vedtakId);

        List<DialogMelding> dialogMeldinger = meldingRepository.hentDialogMeldinger(vedtakId);
        List<SystemMelding> systemMeldinger = meldingRepository.hentSystemMeldinger(vedtakId);

        assertTrue(dialogMeldinger.isEmpty());
        assertTrue(systemMeldinger.isEmpty());
    }


}
