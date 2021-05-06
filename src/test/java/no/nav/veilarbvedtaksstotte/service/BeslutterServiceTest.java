package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.person.PersonNavn;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.UtkastetVedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.*;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class BeslutterServiceTest {

    private final VedtaksstotteRepository vedtaksstotteRepository = mock(VedtaksstotteRepository.class);
    private final VedtakStatusEndringService vedtakStatusEndringService = mock(VedtakStatusEndringService.class);
    private final BeslutteroversiktRepository beslutteroversiktRepository = mock(BeslutteroversiktRepository.class);
    private final MeldingRepository meldingRepository = mock(MeldingRepository.class);
    private final VeilederService veilederService = mock(VeilederService.class);
    private final VeilarbpersonClient veilarbpersonClient = mock(VeilarbpersonClient.class);
    private final AuthService authService = mock(AuthService.class);
    private final JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
    private final TransactionTemplate transactor = new TransactionTemplate(new DataSourceTransactionManager(Objects.requireNonNull(db.getDataSource())));
    private final BeslutterService beslutterService = new BeslutterService(
            authService, vedtaksstotteRepository, vedtakStatusEndringService,
            beslutteroversiktRepository, meldingRepository, veilederService, veilarbpersonClient, transactor
    );

    @Before
    public void setup() {
        doReturn(TEST_VEILEDER_IDENT).when(authService).getInnloggetVeilederIdent();
        doReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID)).when(authService).sjekkTilgangTilBrukerOgEnhet(any(Fnr.class));
        doReturn(TEST_FNR).when(authService).getFnrOrThrow(TEST_AKTOR_ID);

        when(veilederService.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder().setIdent(TEST_VEILEDER_IDENT).setNavn("VEILEDER"));
        when(veilederService.hentEnhetNavn(anyString())).thenReturn(TEST_OPPFOLGINGSENHET_NAVN);
        when(veilarbpersonClient.hentPersonNavn(TEST_FNR.get())).thenReturn(new PersonNavn("FORNAVN", null, "ETTERNAVN", null));
    }

    @Test
    public void startBeslutterProsess__skal_starte_beslutter_prosess_hvis_ikke_startet() {
        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .aktorId(TEST_AKTOR_ID)
                .innsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .oppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .veilederIdent(TEST_VEILEDER_IDENT)
                .build();

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.startBeslutterProsess(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(KLAR_TIL_BESLUTTER));
    }

    @Test
    public void startBeslutterProsess__skal_ikke_starte_beslutter_prosess_hvis_allerede_startet() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakMedBeslutterProsessOgInnsatsGruppe(
                Innsatsgruppe.VARIG_TILPASSET_INNSATS
        );

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.startBeslutterProsess(SOME_ID));
    }

    @Test
    public void startBeslutterProsess__skal_kaste_exception_hvis_feil_innsatsgruppe() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakForInnsatsGruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.startBeslutterProsess(SOME_ID));
    }

    @Test
    public void startBeslutterProsess__skal_opprette_system_melding() {
        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .aktorId(TEST_AKTOR_ID)
                .innsatsgruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS)
                .oppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .veilederIdent(TEST_VEILEDER_IDENT)
                .build();

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.startBeslutterProsess(SOME_ID);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BESLUTTER_PROSESS_STARTET), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void avbrytBeslutterProsess__skal_avbryte_beslutter_prosess_hvis_ikke_allerede_gjort() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakMedBeslutterProsessOgInnsatsGruppe(
                Innsatsgruppe.STANDARD_INNSATS
        );

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.avbrytBeslutterProsess(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(null));
    }

    @Test
    public void avbrytBeslutterProsess__skal_kaste_exception_hvis_feil_innsatsgruppe() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakForInnsatsGruppe(Innsatsgruppe.VARIG_TILPASSET_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.avbrytBeslutterProsess(SOME_ID));
    }

    @Test
    public void avbrytBeslutterProsess__skal_opprette_system_melding() {
        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .aktorId(TEST_AKTOR_ID)
                .beslutterProsessStatus(KLAR_TIL_BESLUTTER)
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .oppfolgingsenhetId(TEST_OPPFOLGINGSENHET_ID)
                .veilederIdent(TEST_VEILEDER_IDENT)
                .build();

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.avbrytBeslutterProsess(SOME_ID);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BESLUTTER_PROSESS_AVBRUTT), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter__skal_ta_over_som_beslutter() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakForInnsatsGruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.bliBeslutter(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutter(anyLong(), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter__skal_kaste_exception_nar_veileder_er_ansvarlig() {
        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .aktorId(TEST_AKTOR_ID)
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .veilederIdent(TEST_VEILEDER_IDENT)
                .build();

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.bliBeslutter(SOME_ID));
    }

    @Test
    public void bliBeslutter__skal_ikke_endres_hvis_allerede_beslutter() {
        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .aktorId(TEST_AKTOR_ID)
                .beslutterIdent(TEST_VEILEDER_IDENT)
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .build();

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.bliBeslutter(SOME_ID));
    }

    @Test
    public void bliBeslutter__skal_opprette_system_melding_bli_beslutter() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakForInnsatsGruppe(Innsatsgruppe.STANDARD_INNSATS);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.bliBeslutter(SOME_ID);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BLITT_BESLUTTER), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void bliBeslutter__skal_opprette_system_melding_overta_for_beslutter() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakMedInnsatsOgBeslutterIdent("TIDLIGERE BESLUTTER IDENT");

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.bliBeslutter(SOME_ID);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.TATT_OVER_SOM_BESLUTTER), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void godkjennVedtak__skal_sette_godkjent() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakMedInnsatsOgBeslutterIdent(TEST_VEILEDER_IDENT);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.setGodkjentAvBeslutter(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(GODKJENT_AV_BESLUTTER));
    }

    @Test
    public void godkjennVedtak__skal_kaste_exception_hvis_ikke_beslutter() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakMedInnsatsOgBeslutterIdent(TEST_BESLUTTER_IDENT);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.setGodkjentAvBeslutter(SOME_ID));
    }

    @Test
    public void godkjennVedtak__skal_opprette_system_melding() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakMedInnsatsOgBeslutterIdent(TEST_VEILEDER_IDENT);

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.setGodkjentAvBeslutter(SOME_ID);

        verify(meldingRepository, times(1))
                .opprettSystemMelding(anyLong(), eq(SystemMeldingType.BESLUTTER_HAR_GODKJENT), eq(TEST_VEILEDER_IDENT));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_sette_riktig_status_for_veileder() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakMedBeslutterIdentOgProsessStatusOgVeilerIdent(
                KLAR_TIL_VEILEDER
        );

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.oppdaterBeslutterProsessStatus(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(KLAR_TIL_BESLUTTER));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_sette_riktig_status_for_beslutter() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakMedBeslutterIdentOgProsessStatusOgVeilerIdent(
                KLAR_TIL_BESLUTTER
        );

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_BESLUTTER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        beslutterService.oppdaterBeslutterProsessStatus(SOME_ID);

        verify(vedtaksstotteRepository, times(1)).setBeslutterProsessStatus(anyLong(), eq(KLAR_TIL_VEILEDER));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_feile_hvis_status_er_lik() {
        UtkastetVedtak utkastetVedtak = UtkastetVedtak.builder()
                .aktorId(TEST_AKTOR_ID)
                .beslutterIdent(TEST_BESLUTTER_IDENT)
                .beslutterProsessStatus(KLAR_TIL_VEILEDER)
                .build();

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_BESLUTTER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.oppdaterBeslutterProsessStatus(SOME_ID));
    }

    @Test
    public void oppdaterBeslutterProsessStatus__skal_feile_hvis_bruker_ikke_er_beslutter_eller_veileder() {
        UtkastetVedtak utkastetVedtak = getUtkastetVedtakMedBeslutterIdentOgProsessStatusOgVeilerIdent(
                KLAR_TIL_VEILEDER
        );

        when(authService.sjekkTilgangTilBrukerOgEnhet(TEST_FNR)).thenReturn(new AuthKontekst().setAktorId(TEST_AKTOR_ID));
        when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_IKKE_ANSVARLIG_VEILEDER_IDENT);
        when(vedtaksstotteRepository.hentUtkastEllerFeil(SOME_ID)).thenReturn(utkastetVedtak);

        assertThrows(ResponseStatusException.class, () -> beslutterService.oppdaterBeslutterProsessStatus(SOME_ID));
    }

    private UtkastetVedtak getUtkastetVedtakMedBeslutterIdentOgProsessStatusOgVeilerIdent(
            BeslutterProsessStatus beslutterProsessStatus) {
        return UtkastetVedtak.builder()
                .aktorId(TEST_AKTOR_ID)
                .beslutterIdent(no.nav.veilarbvedtaksstotte.utils.TestData.TEST_BESLUTTER_IDENT)
                .beslutterProsessStatus(beslutterProsessStatus)
                .veilederIdent(no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT)
                .build();
    }

    private UtkastetVedtak getUtkastetVedtakForInnsatsGruppe(Innsatsgruppe innsatsgruppe) {
        return UtkastetVedtak.builder()
                .aktorId(TEST_AKTOR_ID)
                .innsatsgruppe(innsatsgruppe)
                .build();
    }

    private UtkastetVedtak getUtkastetVedtakMedInnsatsOgBeslutterIdent(
            String beslutterIdent) {
        return UtkastetVedtak.builder()
                .aktorId(TEST_AKTOR_ID)
                .innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .beslutterIdent(beslutterIdent)
                .build();
    }

    private UtkastetVedtak getUtkastetVedtakMedBeslutterProsessOgInnsatsGruppe(
            Innsatsgruppe innsatsgruppe
    ) {
        return UtkastetVedtak.builder()
                .aktorId(TEST_AKTOR_ID)
                .beslutterProsessStatus(BeslutterProsessStatus.KLAR_TIL_BESLUTTER)
                .innsatsgruppe(innsatsgruppe)
                .build();
    }
}
