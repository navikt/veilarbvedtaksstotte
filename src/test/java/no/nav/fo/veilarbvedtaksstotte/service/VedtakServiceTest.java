package no.nav.fo.veilarbvedtaksstotte.service;

import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.fo.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.fo.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import static no.nav.fo.veilarbvedtaksstotte.utils.TestData.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VedtakServiceTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static JdbcTemplate db;

    private static VedtaksstotteRepository vedtaksstotteRepository;
    private static KilderRepository kilderRepository;
    private static VedtakService vedtakService;
    private static VeilederService veilederService = mock(VeilederService.class);
    private static AuthService authSerivce = mock(AuthService.class);


    @BeforeClass
    public static void setup() {
        db = DbTestUtils.setupEmbeddedDb(pg.getEmbeddedPostgres());
        kilderRepository = new KilderRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository);

        vedtakService = new VedtakService(vedtaksstotteRepository,
                kilderRepository,
                null,
                authSerivce,
                null,
                null,
                veilederService,
                null,
                null,
                null,
                null);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void taOverUtkast__setter_ny_veileder() {
        String nyVeilederident = TEST_VEILEDER_IDENT + "ny";
        when(authSerivce.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst(Bruker.fraFnr(TEST_FNR).medAktoerId(TEST_AKTOR_ID), TEST_VEILEDER_ENHET_ID));
        when(veilederService.hentVeilederIdentFraToken()).thenReturn(nyVeilederident);
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

        assertEquals(TEST_VEILEDER_IDENT, vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getVeilederIdent());
        vedtakService.taOverUtkast(TEST_FNR);
        assertEquals(nyVeilederident, vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getVeilederIdent());
    }

    @Test
    public void taOverUtkast__feiler_dersom_ikke_utkast() {
        when(authSerivce.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst(Bruker.fraFnr(TEST_FNR).medAktoerId(TEST_AKTOR_ID), TEST_VEILEDER_ENHET_ID));

        assertThatThrownBy(() ->
                vedtakService.taOverUtkast(TEST_FNR)
        ).isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    public void taOverUtkast__feiler_dersom_ingen_tilgang() {
        when(authSerivce.sjekkTilgang(TEST_FNR)).thenThrow(new IngenTilgang());

        assertThatThrownBy(() ->
                vedtakService.taOverUtkast(TEST_FNR)
        ).isExactlyInstanceOf(IngenTilgang.class);
    }

    @Test
    public void taOverUtkast__feiler_dersom_samme_veileder() {
        when(authSerivce.sjekkTilgang(TEST_FNR)).thenReturn(new AuthKontekst(Bruker.fraFnr(TEST_FNR).medAktoerId(TEST_AKTOR_ID), TEST_VEILEDER_ENHET_ID));
        when(veilederService.hentVeilederIdentFraToken()).thenReturn(TEST_VEILEDER_IDENT);
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

        assertThatThrownBy(() ->
                vedtakService.taOverUtkast(TEST_FNR)
        ).isExactlyInstanceOf(BadRequestException.class);
    }
}
