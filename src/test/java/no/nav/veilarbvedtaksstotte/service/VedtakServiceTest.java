package no.nav.veilarbvedtaksstotte.service;

import io.getunleash.DefaultUnleash;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.poao_tilgang.client.Decision;
import no.nav.poao_tilgang.client.PoaoTilgangClient;
import no.nav.poao_tilgang.client.api.ApiResult;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.request.EgenvurderingForPersonRequest;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ArbeidssoekerRegisteretService;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.OpplysningerOmArbeidssoekerMedProfilering;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.arena.dto.VeilarbArenaOppfolging;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.Journalpost;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.JournalpostGraphqlResponse;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettetJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient;
import no.nav.veilarbvedtaksstotte.client.dokdistkanal.DokdistkanalClient;
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon;
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetStedsadresse;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.dto.*;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.Adresse;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.SakDTO;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder;
import no.nav.veilarbvedtaksstotte.config.EnvironmentProperties;
import no.nav.veilarbvedtaksstotte.controller.dto.OppdaterUtkastDTO;
import no.nav.veilarbvedtaksstotte.controller.dto.SlettVedtakRequest;
import no.nav.veilarbvedtaksstotte.domain.Malform;
import no.nav.veilarbvedtaksstotte.domain.VedtakOpplysningKilder;
import no.nav.veilarbvedtaksstotte.domain.arkiv.BrevKode;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.EgenvurderingDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingMetode;
import no.nav.veilarbvedtaksstotte.domain.vedtak.*;
import no.nav.veilarbvedtaksstotte.repository.*;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_DOKUMENT_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_JOURNALPOST_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_KILDER;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_NAVN;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_NAVN;
import static no.nav.veilarbvedtaksstotte.utils.TestUtils.readTestResourceFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VedtakServiceTest extends DatabaseTest {

    private static VedtaksstotteRepository vedtaksstotteRepository;
    private static KilderRepository kilderRepository;
    private static MeldingRepository meldingRepository;
    private static SakStatistikkRepository sakStatistikkRepository;
    private static VedtakService vedtakService;
    private static OyeblikksbildeService oyeblikksbildeService;
    private static AuthService authService;

    private static final DefaultUnleash unleashService = mock(DefaultUnleash.class);

    private static final LeaderElectionClient leaderElectionClient = mock(LeaderElectionClient.class);
    private static final VedtakHendelserService vedtakHendelserService = mock(VedtakHendelserService.class);
    private static final VeilederService veilederService = mock(VeilederService.class);
    private static final VeilarbpersonClient veilarbpersonClient = mock(VeilarbpersonClient.class);
    private static final ArbeidssoekerRegisteretService arbeidssoekerRegistretService = mock(ArbeidssoekerRegisteretService.class);
    private static final AiaBackendClient aia_backend_client = mock(AiaBackendClient.class);

    private static final RegoppslagClient regoppslagClient = mock(RegoppslagClient.class);
    private static final AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);
    private static final VeilarbarenaClient veilarbarenaClient = mock(VeilarbarenaClient.class);
    private static final DokarkivClient dokarkivClient = mock(DokarkivClient.class);
    private static final DokdistribusjonClient dokdistribusjonClient = mock(DokdistribusjonClient.class);
    private static final DokdistkanalClient dokdistkanalClient = mock(DokdistkanalClient.class);
    private static final VeilarbveilederClient veilarbveilederClient = mock(VeilarbveilederClient.class);
    private static final EnhetInfoService enhetInfoService = mock(EnhetInfoService.class);

    private static final SafClient safClient = mock(SafClient.class);
    private static final MetricsService metricsService = mock(MetricsService.class);
    private static final PoaoTilgangClient poaoTilgangClient = mock(PoaoTilgangClient.class);
    private static final PdfService pdfService = mock(PdfService.class);
    private static final VeilarboppfolgingClient veilarboppfolgingClient = mock(VeilarboppfolgingClient.class);
    private static final BigQueryService bigQueryService = mock(BigQueryService.class);
    private static final EnvironmentProperties environmentProperties = mock(EnvironmentProperties.class);
    private static final Gjeldende14aVedtakService gjeldende14aVedtakService = mock(Gjeldende14aVedtakService.class);
    private static final KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);

    @BeforeAll
    public static void setupOnce() {
        VeilarbarenaService veilarbarenaService = new VeilarbarenaService(veilarbarenaClient);
        kilderRepository = spy(new KilderRepository(jdbcTemplate));
        meldingRepository = spy(new MeldingRepository(jdbcTemplate));
        vedtaksstotteRepository = new VedtaksstotteRepository(jdbcTemplate, transactor);
        sakStatistikkRepository = new SakStatistikkRepository(jdbcTemplate);
        RetryVedtakdistribusjonRepository retryVedtakdistribusjonRepository = new RetryVedtakdistribusjonRepository(jdbcTemplate);
        OyeblikksbildeRepository oyeblikksbildeRepository = new OyeblikksbildeRepository(jdbcTemplate);
        BeslutteroversiktRepository beslutteroversiktRepository = new BeslutteroversiktRepository(jdbcTemplate);
        authService = spy(new AuthService(aktorOppslagClient, veilarbarenaService, AuthContextHolderThreadLocal.instance(), poaoTilgangClient));
        SakStatistikkService sakStatistikkService = new SakStatistikkService(sakStatistikkRepository, veilarboppfolgingClient, aktorOppslagClient, bigQueryService, environmentProperties, veilarbpersonClient);

        oyeblikksbildeService = new OyeblikksbildeService(authService, oyeblikksbildeRepository, vedtaksstotteRepository, veilarbpersonClient, aia_backend_client, arbeidssoekerRegistretService);
        MalTypeService malTypeService = new MalTypeService(arbeidssoekerRegistretService);
        DokumentService dokumentService = new DokumentService(
                veilarboppfolgingClient,
                veilarbpersonClient,
                dokarkivClient,
                malTypeService,
                oyeblikksbildeService,
                pdfService
        );
        DistribusjonService distribusjonService = new DistribusjonService(
                vedtaksstotteRepository,
                retryVedtakdistribusjonRepository,
                dokdistribusjonClient,
                dokdistkanalClient);
        vedtakService = new VedtakService(
                transactor,
                vedtaksstotteRepository,
                beslutteroversiktRepository,
                kilderRepository,
                meldingRepository,
                safClient,
                authService,
                oyeblikksbildeService,
                veilederService,
                vedtakHendelserService,
                dokumentService,
                distribusjonService,
                metricsService,
                leaderElectionClient,
                sakStatistikkService,
                aktorOppslagClient,
                veilarboppfolgingClient,
                gjeldende14aVedtakService,
                kafkaProducerService,
                unleashService
        );
    }

    @BeforeEach
    public void setup() {
        DbTestUtils.cleanupDb(jdbcTemplate);
        reset(veilederService);
        reset(meldingRepository);
        reset(unleashService);
        reset(dokarkivClient);
        reset(vedtakHendelserService);
        doReturn(TEST_VEILEDER_IDENT).when(authService).getInnloggetVeilederIdent();
        doReturn(UUID.randomUUID()).when(authService).hentInnloggetVeilederUUID();
        when(veilederService.hentEnhetNavn(TEST_OPPFOLGINGSENHET_ID)).thenReturn(TEST_OPPFOLGINGSENHET_NAVN);
        when(veilederService.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder(TEST_VEILEDER_IDENT, TEST_VEILEDER_NAVN));
        when(veilederService.hentVeilederEllerNull(TEST_VEILEDER_IDENT)).thenReturn(Optional.of(new Veileder(TEST_VEILEDER_IDENT, TEST_VEILEDER_NAVN)));
        when(regoppslagClient.hentPostadresse(any())).thenReturn(
                new RegoppslagResponseDTO("", new Adresse(NORSKPOSTADRESSE, "", "", "", "", "", "", "")));
        when(veilarbpersonClient.hentAdressebeskyttelse(TEST_FNR)).thenReturn(new Adressebeskyttelse(Gradering.UGRADERT));
        when(veilarbpersonClient.hentMalform(TEST_FNR)).thenReturn(Malform.NB);
        when(veilarbpersonClient.hentCVOgJobbprofil(TEST_FNR.get())).thenReturn(new CvDto.CVMedInnhold(JsonUtils.fromJson(testCvData(), CvInnhold.class)));
        when(veilarbpersonClient.hentPersonNavn(TEST_FNR.get())).thenReturn(new PersonNavn("Fornavn", null, "Etternavn", null));
        when(veilarbpersonClient.hentPersonNavnForJournalforing(TEST_FNR.get())).thenReturn(new PersonNavn("Fornavn", null, "Etternavn", null));
        when(veilarbpersonClient.hentFodselsdato(TEST_FNR)).thenReturn(new FodselsdatoOgAr(LocalDate.of(1990, 3, 12), 1990));
        when(aia_backend_client.hentEgenvurdering(new EgenvurderingForPersonRequest(TEST_FNR.get()))).thenReturn(JsonUtils.fromJson(testEgenvurderingData(), EgenvurderingResponseDTO.class));
        when(aktorOppslagClient.hentAktorId(TEST_FNR)).thenReturn(AktorId.of(TEST_AKTOR_ID));
        when(aktorOppslagClient.hentFnr(AktorId.of(TEST_AKTOR_ID))).thenReturn(TEST_FNR);
        when(veilarbarenaClient.hentOppfolgingsbruker(TEST_FNR)).thenReturn(Optional.of(new VeilarbArenaOppfolging(TEST_OPPFOLGINGSENHET_ID, "ARBS", "IKVAL")));
        when(veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(any())).thenReturn(Optional.of(new OppfolgingPeriodeDTO(UUID.randomUUID(), ZonedDateTime.now(), null)));
        when(veilarboppfolgingClient.hentOppfolgingsperiodeSak(any())).thenReturn(new SakDTO(UUID.randomUUID(), 12345, "ARBEIDSOPPFOLGING", "OPP"));
        when(veilarboppfolgingClient.erUnderOppfolging(any())).thenReturn(Optional.of(true));
        when(dokarkivClient.opprettJournalpost(any()))
                .thenReturn(new OpprettetJournalpostDTO(
                        TEST_JOURNALPOST_ID,
                        true,
                        List.of(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID))));
        when(veilarbveilederClient.hentVeileder(TEST_VEILEDER_IDENT)).thenReturn(new Veileder(TEST_VEILEDER_IDENT, TEST_VEILEDER_NAVN));
        when(enhetInfoService.hentEnhet(EnhetId.of(TEST_OPPFOLGINGSENHET_ID))).thenReturn(new Enhet().setNavn(TEST_OPPFOLGINGSENHET_NAVN));
        when(enhetInfoService.utledEnhetKontaktinformasjon(EnhetId.of(TEST_OPPFOLGINGSENHET_ID)))
                .thenReturn(new EnhetKontaktinformasjon(EnhetId.of(TEST_OPPFOLGINGSENHET_ID), new EnhetStedsadresse("", "", "", "", "", ""), ""));
        when(pdfService.produserDokument(any())).thenReturn(new byte[]{});
        when(pdfService.produserCVPdf(any(), any())).thenReturn(Optional.of(new byte[]{}));
        when(pdfService.produserBehovsvurderingPdf(any(), any())).thenReturn(Optional.of(new byte[]{}));
        when(poaoTilgangClient.evaluatePolicy(any())).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));
        when(safClient.hentJournalpost(any())).thenReturn(getMockedJournalpostGraphqlResponse());
    }

    @Test
    public void fattVedtak__opprett_oppdater_og_journalforer_og_ferdigstiller_vedtak() {

        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            assertNyttUtkast();
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            OppdaterUtkastDTO oppdaterDto = new OppdaterUtkastDTO()
                    .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                    .setBegrunnelse("En begrunnelse")
                    .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                    .setOpplysninger(List.of(
                            VedtakOpplysningKilder.REGISTRERING.getDesc(),
                            VedtakOpplysningKilder.EGENVURDERING.getDesc(),
                            VedtakOpplysningKilder.CV.getDesc(),
                            VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET.getDesc()
                    ));

            vedtakService.oppdaterUtkast(utkast.getId(), oppdaterDto);
            assertOppdatertUtkast(oppdaterDto);

            vedtakService.fattVedtak(utkast.getId());
            assertJournalfortOgFerdigstiltVedtak();
        });
    }

    @Test
    public void fattVedtak__opprett_oppdater_og_send_vedtak() {
        gittUtkastKlarForUtsendelse();

        fattVedtak();

        assertJournalfortOgFerdigstiltVedtak();
    }

    @Test
    public void lagUtkast__skal_opprette_system_melding() {
        withContext(() -> {
            gittTilgang();
            vedtakService.lagUtkast(TEST_FNR);

            verify(meldingRepository, times(1))
                    .opprettSystemMelding(anyLong(), eq(SystemMeldingType.UTKAST_OPPRETTET), eq(TEST_VEILEDER_IDENT));
        });
    }

    @Test
    public void oppdaterUtkast__skal_ikke_endre_kilder_hvis_ikke_endret() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            List<String> kilder = List.of(VedtakOpplysningKilder.REGISTRERING.getDesc(), VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET.getDesc(), VedtakOpplysningKilder.CV.getDesc());
            kilderRepository.lagKilder(kilder, utkast.getId());

            OppdaterUtkastDTO oppdaterUtkastDTO = new OppdaterUtkastDTO();
            oppdaterUtkastDTO.setOpplysninger(kilder);

            vedtakService.oppdaterUtkast(utkast.getId(), oppdaterUtkastDTO);

            verify(kilderRepository, never())
                    .slettKilder(utkast.getId());

            verify(kilderRepository, times(1))
                    .lagKilder(kilder, utkast.getId());
        });
    }

    @Test
    public void oppdaterUtkast__skal_endre_kilder_hvis_endret() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            List<String> gamleKilder = List.of(VedtakOpplysningKilder.REGISTRERING.getDesc(), VedtakOpplysningKilder.EGENVURDERING.getDesc());
            List<String> nyeKilder = List.of(VedtakOpplysningKilder.EGENVURDERING.getDesc(), VedtakOpplysningKilder.CV.getDesc(), VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET.getDesc());
            kilderRepository.lagKilder(gamleKilder, utkast.getId());

            OppdaterUtkastDTO oppdaterUtkastDTO = new OppdaterUtkastDTO();
            oppdaterUtkastDTO.setOpplysninger(nyeKilder);

            vedtakService.oppdaterUtkast(utkast.getId(), oppdaterUtkastDTO);

            verify(kilderRepository, times(1))
                    .slettKilder(utkast.getId());

            verify(kilderRepository, times(1))
                    .lagKilder(nyeKilder, utkast.getId());
        });
    }

    @Test
    public void oppdaterUtkast__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() -> {
                        vedtakService.fattVedtak(utkast.getId());
                        vedtakService.oppdaterUtkast(utkast.getId(), new OppdaterUtkastDTO());
                    }
            ).isExactlyInstanceOf(ResponseStatusException.class);
        });
    }

    @Test
    public void slettUtkast__skal_slette_utkast_med_data() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        kilderRepository.lagKilder(TEST_KILDER, utkast.getId());

        meldingRepository.opprettDialogMelding(utkast.getId(), null, "Test");

        vedtakService.slettUtkast(utkast, BehandlingMetode.MANUELL);

        assertNull(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID));
    }

    @Test
    public void slettUtkast__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            assertThatThrownBy(() ->
                    vedtakService.slettUtkastSomVeileder(utkast.getId())
            ).isExactlyInstanceOf(ResponseStatusException.class);
        });
    }

    @Test
    public void fattVedtak__journalforer_og_ferdigstiller_vedtak() {
        gittUtkastKlarForUtsendelse();

        when(dokarkivClient.opprettJournalpost(any()))
                .thenReturn(new OpprettetJournalpostDTO(
                        TEST_JOURNALPOST_ID,
                        false,
                        List.of(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID))));

        fattVedtak();

        assertJournalfortOgFerdigstiltVedtak();
    }

    @Test
    public void fattVedtak__feiler_for_veileder_som_ikke_er_satt_pa_utkast() {
        withContext(() -> {
            gittTilgang();

            vedtakService.lagUtkast(TEST_FNR);

            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            vedtakService.oppdaterUtkast(utkast.getId(),
                    new OppdaterUtkastDTO()
                            .setBegrunnelse("begrunnelse")
                            .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                            .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                            .setOpplysninger(Collections.singletonList(VedtakOpplysningKilder.CV.getDesc())));

            when(authService.getInnloggetVeilederIdent()).thenReturn(TEST_VEILEDER_IDENT + "annen");

            assertThatThrownBy(() ->
                    vedtakService.fattVedtak(utkast.getId())
            ).isExactlyInstanceOf(ResponseStatusException.class);
        });
    }

    @Test
    public void taOverUtkast__setter_ny_veileder() {
        withContext(() -> {
            String tidligereVeilederId = TEST_VEILEDER_IDENT + "tidligere";
            gittTilgang();
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, tidligereVeilederId, TEST_OPPFOLGINGSENHET_ID);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            assertEquals(tidligereVeilederId, vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getVeilederIdent());
            vedtakService.taOverUtkast(utkast.getId());
            assertEquals(TEST_VEILEDER_IDENT, vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getVeilederIdent());
        });
    }

    @Test
    public void taOverUtkast__fjerner_beslutter_hvis_veileder_er_beslutter() {
        withContext(() -> {
            gittTilgang();
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT + "tidligere", TEST_OPPFOLGINGSENHET_ID);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
            vedtaksstotteRepository.setBeslutter(utkast.getId(), TEST_VEILEDER_IDENT);

            vedtakService.taOverUtkast(utkast.getId());
            assertNull(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getBeslutterIdent());
        });
    }

    @Test
    public void taOverUtkast__oppretter_system_melding() {
        withContext(() -> {
            String tidligereVeilederId = TEST_VEILEDER_IDENT + "tidligere";
            gittTilgang();

            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, tidligereVeilederId, TEST_OPPFOLGINGSENHET_ID);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            reset(meldingRepository);

            vedtakService.taOverUtkast(utkast.getId());

            verify(meldingRepository, times(1))
                    .opprettSystemMelding(anyLong(), eq(SystemMeldingType.TATT_OVER_SOM_VEILEDER), eq(TEST_VEILEDER_IDENT));
        });
    }

    @Test
    public void taOverUtkast__feiler_dersom_ikke_utkast() {
        withContext(() -> assertThatThrownBy(() ->
                vedtakService.taOverUtkast(123)
        ).hasMessage("404 NOT_FOUND \"Fant ikke utkast\""));
    }

    @Test
    public void taOverUtkast__feiler_dersom_ingen_tilgang() {
        assertThatThrownBy(() ->
                vedtakService.taOverUtkast(123)
        ).isExactlyInstanceOf(ResponseStatusException.class);
    }

    @Test
    public void taOverUtkast__feiler_dersom_samme_veileder() {
        withContext(() -> {
            gittTilgang();
            vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            assertThatThrownBy(() ->
                    vedtakService.taOverUtkast(utkast.getId())
            ).isExactlyInstanceOf(ResponseStatusException.class);
        });
    }

    @Test
    void slett_vedtak_ved_personvernsbrudd() {
        gittUtkastKlarForUtsendelse();

        when(dokarkivClient.opprettJournalpost(any()))
                .thenReturn(new OpprettetJournalpostDTO(
                        TEST_JOURNALPOST_ID,
                        false,
                        List.of(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID))));

        fattVedtak();

        assertJournalfortOgFerdigstiltVedtak();
        assertNotNull(vedtaksstotteRepository.hentFattedeVedtakInkludertSlettede(TEST_AKTOR_ID).getFirst().getBegrunnelse());

        SlettVedtakRequest slettVedtakRequest = new SlettVedtakRequest(TEST_JOURNALPOST_ID, TEST_FNR, NavIdent.of(TEST_VEILEDER_IDENT), "FAGSYSTEM-12234555");
        vedtakService.slettVedtak(slettVedtakRequest, NavIdent.of("Z123456"));
        assertNull(vedtaksstotteRepository.hentFattedeVedtakInkludertSlettede(TEST_AKTOR_ID).getFirst().getBegrunnelse());
    }

    @Test
    void ikke_vedtak_naar_personen_ikke_er_under_oppfolging() {
        withContext(() -> {
            gittTilgang();
            gittUtkastKlarForUtsendelse();
            when(veilarboppfolgingClient.erUnderOppfolging(any())).thenReturn(Optional.of(false));

            assertThatThrownBy(() -> vedtakService.fattVedtak(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId())
            ).isExactlyInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Bruker er ikke under oppfølging og kan ikke få vedtak");
        });
    }

    private void gittTilgang() {
        when(poaoTilgangClient.evaluatePolicy(any())).thenReturn(new ApiResult<>(null, Decision.Permit.INSTANCE));
    }

    private void withContext(UnsafeRunnable runnable) {
        AuthContextHolderThreadLocal
                .instance()
                .withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, TEST_VEILEDER_IDENT), runnable);
    }

    private void gittUtkastKlarForUtsendelse() {
        withContext(() -> {
            gittTilgang();
            vedtakService.lagUtkast(TEST_FNR);
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

            OppdaterUtkastDTO oppdaterDto = new OppdaterUtkastDTO()
                    .setHovedmal(Hovedmal.SKAFFE_ARBEID)
                    .setBegrunnelse("En begrunnelse")
                    .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                    .setOpplysninger(Arrays.asList(VedtakOpplysningKilder.CV.getDesc(), VedtakOpplysningKilder.EGENVURDERING.getDesc(), VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET.getDesc()));

            List<String> kilder = List.of("CV-en/jobbønskene dine på nav.no", "Svarene dine om behov for veiledning", "Svarene dine fra da du registrerte deg");

            kilderRepository.lagKilder(kilder, utkast.getId());

            vedtakService.oppdaterUtkast(utkast.getId(), oppdaterDto);

        });
    }

    private void assertNyttUtkast() {
        Vedtak opprettetUtkast = vedtakService.hentUtkast(TEST_FNR);
        assertEquals(VedtakStatus.UTKAST, opprettetUtkast.getVedtakStatus());
        assertEquals(TEST_VEILEDER_IDENT, opprettetUtkast.getVeilederIdent());
        assertEquals(TEST_VEILEDER_NAVN, opprettetUtkast.getVeilederNavn());
        assertEquals(TEST_OPPFOLGINGSENHET_ID, opprettetUtkast.getOppfolgingsenhetId());
        assertEquals(TEST_OPPFOLGINGSENHET_NAVN, opprettetUtkast.getOppfolgingsenhetNavn());
        assertFalse(opprettetUtkast.isGjeldende());
        assertEquals(0, opprettetUtkast.getKilder().size());
        assertFalse(opprettetUtkast.isSender());
    }

    private Vedtak hentVedtak() {
        List<Vedtak> vedtakList = vedtakService.hentFattedeVedtak(TEST_FNR);
        assertEquals(1, vedtakList.size());
        return vedtakList.getFirst();
    }

    private void fattVedtak() {
        withContext(() -> {
            gittTilgang();
            Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
            vedtakService.fattVedtak(utkast.getId());
        });
    }

    private void assertOppdatertUtkast(OppdaterUtkastDTO dto) {
        Vedtak oppdatertUtkast = vedtakService.hentUtkast(TEST_FNR);
        assertEquals(dto.getHovedmal(), oppdatertUtkast.getHovedmal());
        assertEquals(dto.getBegrunnelse(), oppdatertUtkast.getBegrunnelse());
        assertEquals(dto.getInnsatsgruppe(), oppdatertUtkast.getInnsatsgruppe());
        List<String> oppdaterteKilderActual = oppdatertUtkast.getKilder().stream().map(KildeEntity::getTekst).toList();
        List<String> oppdaterteKilderExpected = dto.getOpplysninger();
        assertThat(oppdaterteKilderActual).containsExactlyInAnyOrderElementsOf(oppdaterteKilderExpected);
    }

    private void assertJournalfortOgFerdigstiltVedtak() {
        withContext(() -> {
            gittTilgang();
            Vedtak sendtVedtak = hentVedtak();
            assertTrue(sendtVedtak.isGjeldende());
            assertEquals(VedtakStatus.SENDT, sendtVedtak.getVedtakStatus());
            assertEquals(TEST_DOKUMENT_ID, sendtVedtak.getDokumentInfoId());
            assertEquals(TEST_JOURNALPOST_ID, sendtVedtak.getJournalpostId());
            assertOyeblikksbildeForFattetVedtak(sendtVedtak.getId());

            sakStatistikkRepository.hentSakStatistikkListe(sendtVedtak.getAktorId());
        });
        verify(vedtakHendelserService).vedtakSendt(any());
    }

    private void assertOyeblikksbildeForFattetVedtak(long vedtakId) {
        withContext(() -> {
            List<OyeblikksbildeDto> oyeblikksbilde = oyeblikksbildeService.hentOyeblikksbildeForVedtak(vedtakId);
            assertTrue(oyeblikksbilde.stream().filter(x -> x.oyeblikksbildeType == OyeblikksbildeType.REGISTRERINGSINFO).map(x -> JsonUtils.fromJson(x.getJson(), OpplysningerOmArbeidssoekerMedProfilering.class)).allMatch(x -> x.equals(JsonUtils.fromJson(getOppdatertRegistreringsdata(), OpplysningerOmArbeidssoekerMedProfilering.class))));
            assertTrue(oyeblikksbilde.stream().filter(x -> x.oyeblikksbildeType == OyeblikksbildeType.CV_OG_JOBBPROFIL).map(x -> JsonUtils.fromJson(x.getJson(), CvInnhold.class)).allMatch(x -> x.equals(JsonUtils.fromJson(testCvData(), CvInnhold.class))));
            assertTrue(oyeblikksbilde.stream().filter(x -> x.oyeblikksbildeType == OyeblikksbildeType.EGENVURDERING).map(x -> JsonUtils.fromJson(x.getJson(), EgenvurderingDto.class)).allMatch(x -> x.equals(JsonUtils.fromJson(testOyeblikkbildeEgenvurderingData(), EgenvurderingDto.class))));
        });
    }

    private String testCvData() {
        return readTestResourceFile("testdata/oyeblikksbilde-cv.json");
    }

    private String getOppdatertRegistreringsdata() {
        return readTestResourceFile("testdata/opplysningerOmArbeidssoekerMedProfilering.json");
    }

    private String testEgenvurderingData() {
        return readTestResourceFile("testdata/egenvurdering-response.json");
    }

    private String testOyeblikkbildeEgenvurderingData() {
        return readTestResourceFile("testdata/egenvurdering-data.json");
    }

    private JournalpostGraphqlResponse getMockedJournalpostGraphqlResponse() {
        JournalpostGraphqlResponse journalpostGraphqlResponse = new JournalpostGraphqlResponse();
        journalpostGraphqlResponse.setData(new JournalpostGraphqlResponse.JournalpostReponseData().setJournalpost(getMockedJournalpost()));
        return journalpostGraphqlResponse;
    }

    private Journalpost getMockedJournalpost() {
        Journalpost journalpost = new Journalpost();
        journalpost.journalpostId = "journalpost123";
        journalpost.tittel = "titel";

        Journalpost.JournalpostDokument journalpostDokument1 = new Journalpost.JournalpostDokument();
        journalpostDokument1.brevkode = BrevKode.EGENVURDERING.name();
        journalpostDokument1.dokumentInfoId = "111111";

        Journalpost.JournalpostDokument journalpostDokument2 = new Journalpost.JournalpostDokument();
        journalpostDokument2.brevkode = BrevKode.REGISTRERINGSINFO.name();
        journalpostDokument2.dokumentInfoId = "222222";

        Journalpost.JournalpostDokument journalpostDokument3 = new Journalpost.JournalpostDokument();
        journalpostDokument3.brevkode = BrevKode.CV_OG_JOBBPROFIL.name();
        journalpostDokument3.dokumentInfoId = "333333";


        journalpost.dokumenter = new Journalpost.JournalpostDokument[]{journalpostDokument1, journalpostDokument2, journalpostDokument3};
        return journalpost;
    }
}
