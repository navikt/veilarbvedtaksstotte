package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.BrukerIdenter;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.request.EgenvurderingForPersonRequest;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.OpplysningerOmArbeidssoekerMedProfilering;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.OppslagArbeidssoekerregisteretClient;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.arena.dto.VeilarbArenaOppfolging;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.Journalpost;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.JournalpostGraphqlResponse;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettetJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.dto.DistribuerJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.dto.DistribuerJournalpostResponsDTO;
import no.nav.veilarbvedtaksstotte.client.dokdistkanal.DokdistkanalClient;
import no.nav.veilarbvedtaksstotte.client.dokdistkanal.dto.BestemDistribusjonskanalResponseDTO;
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon;
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetOrganisering;
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetStedsadresse;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.client.pdf.*;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.dto.*;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagRequestDTO;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.SakDTO;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.domain.Malform;
import org.jetbrains.annotations.NotNull;
import org.joda.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_DOKUMENT_BESTILLING_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_DOKUMENT_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_JOURNALPOST_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_OPPFOLGINGSENHET_NAVN;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_IDENT;

@Configuration
public class ClientTestConfig {

    @Bean
    public AktorOppslagClient aktorOppslagClient() {
        return new AktorOppslagClient() {
            @Override
            public Fnr hentFnr(AktorId aktorId) {
                return TEST_FNR;
            }

            @Override
            public AktorId hentAktorId(Fnr fnr) {
                return AktorId.of(TEST_AKTOR_ID);
            }

            @Override
            public Map<AktorId, Fnr> hentFnrBolk(List<AktorId> list) {
                return Collections.emptyMap();
            }

            @Override
            public Map<Fnr, AktorId> hentAktorIdBolk(List<Fnr> list) {
                return Collections.emptyMap();
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }

            @Override
            public BrukerIdenter hentIdenter(EksternBrukerId eksternBrukerId) {
                return new BrukerIdenter(TEST_FNR, AktorId.of(TEST_AKTOR_ID), emptyList(), emptyList());
            }

        };
    }

    @Bean
    public Norg2Client norg2Client() {
        return new Norg2Client() {

            @Override
            public Enhet hentEnhet(String s) {
                return new Enhet()
                        .setEnhetId(Long.parseLong(TEST_OPPFOLGINGSENHET_ID))
                        .setNavn(TEST_OPPFOLGINGSENHET_NAVN);
            }

            @Override
            public List<Enhet> hentAktiveEnheter() {
                return emptyList();
            }

            @Override
            public EnhetKontaktinformasjon hentKontaktinfo(EnhetId enhetId) {
                return new EnhetKontaktinformasjon(enhetId, new EnhetStedsadresse(null, null, null, null, null, null), "");
            }

            @Override
            public List<EnhetOrganisering> hentEnhetOrganisering(EnhetId enhetId) {
                return emptyList();
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VeilarbarenaClient arenaClient() {
        return new VeilarbarenaClient() {
            @Override
            public Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(Fnr fnr) {
                return Optional.of(new VeilarbArenaOppfolging(TEST_OPPFOLGINGSENHET_ID, "ARBS", "IKVAL"));
            }

            @Override
            public Optional<VeilarbArenaOppfolging> oppdaterOppfolgingsbruker(Fnr fnr, String enhetNr) {
                return Optional.of(new VeilarbArenaOppfolging(TEST_OPPFOLGINGSENHET_ID, "ARBS", "IKVAL"));
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public PdfClient pdfClient() {
        return new PdfClient() {


            @NotNull
            @Override
            public byte[] genererOyeblikksbildeArbeidssokerRegistretPdf(@NotNull OpplysningerOmArbeidssoekerMedProfileringMedMottakerDto registreringOyeblikksbildeData) {
                return new byte[0];
            }

            @NotNull
            @Override
            public byte[] genererOyeblikksbildeEgenVurderingPdf(@NotNull EgenvurderingMedMottakerDto egenvurderingOyeblikksbildeData) {
                return new byte[0];
            }

            @NotNull
            @Override
            public byte[] genererOyeblikksbildeCvPdf(@NotNull CvInnholdMedMottakerDto cvOyeblikksbildeData) {
                return new byte[0];
            }

            @NotNull
            @Override
            public byte[] genererPdf(@NotNull BrevdataDto brevdata) {
                return new byte[0];
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public AiaBackendClient egenvurderingClient() {
        return new AiaBackendClient() {
            @Override
            public EgenvurderingResponseDTO hentEgenvurdering(EgenvurderingForPersonRequest egenvurderingForPersonRequest) {
                Map<String, String> egenvurderingstekster = new HashMap<>();
                egenvurderingstekster.put("STANDARD_INNSATS", "Svar jeg klarer meg");
                return new EgenvurderingResponseDTO("STANDARD_INNSATS", new Instant().toString(), "123456", new EgenvurderingResponseDTO.Tekster("testspm", egenvurderingstekster));
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VeilarboppfolgingClient oppfolgingClient() {
        return new VeilarboppfolgingClient() {
            @Override
            public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(Fnr fnr) {
                OppfolgingPeriodeDTO periode = new OppfolgingPeriodeDTO();
                periode.setStartDato(ZonedDateTime.now().minusDays(10));
                return Collections.singletonList(periode);
            }

            @Override
            public SakDTO hentOppfolgingsperiodeSak(UUID oppfolgingsperiodeId) {
                return new SakDTO(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), 123456789012L, "ARBEIDSOPPFOLGING", "OPP");
            }

            @Override
            public Optional<OppfolgingPeriodeDTO> hentGjeldendeOppfolgingsperiode(Fnr fnr) {
                OppfolgingPeriodeDTO periode = new OppfolgingPeriodeDTO();
                periode.setStartDato(ZonedDateTime.now().minusDays(10));
                return Optional.of(periode);
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VeilarbpersonClient personClient() {
        return new VeilarbpersonClient() {
            @NotNull
            @Override
            public Adressebeskyttelse hentAdressebeskyttelse(@NotNull Fnr fnr) {
                return new Adressebeskyttelse(Gradering.UGRADERT);
            }

            @Override
            public PersonNavn hentPersonNavn(String fnr) {
                PersonNavn personNavn = new PersonNavn(
                        "TEST",
                        null,
                        "TESTERSEN",
                        "TEST TESTERSEN"
                );
                return personNavn;
            }

            @Override
            public PersonNavn hentPersonNavnForJournalforing(String fnr) {
                PersonNavn personNavn = new PersonNavn(
                        "TEST",
                        null,
                        "TESTERSEN",
                        "TEST TESTERSEN"
                );
                return personNavn;
            }

            @Override
            public CvDto hentCVOgJobbprofil(String fnr) {
                return new CvDto.CvMedError(CvErrorStatus.IKKE_DELT);
            }

            @NotNull
            @Override
            public Malform hentMalform(@NotNull Fnr fnr) {
                return Malform.NB;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }

            @Override
            public FodselsdatoOgAr hentFodselsdato(@NotNull Fnr fnr)  {
                return new FodselsdatoOgAr(LocalDate.of(1990, 1, 1), 1990);
            }
        };
    }

    @Bean
    public OppslagArbeidssoekerregisteretClient oppslagArbeidssoekerregisteretClient() {
        return new OppslagArbeidssoekerregisteretClient() {

            @Override
            public HealthCheckResult checkHealth() {
                return null;
            }

            @Override
            public OpplysningerOmArbeidssoekerMedProfilering hentSisteOpplysningerOmArbeidssoekerMedProfilering(Fnr fnr) {
                return null;
            }
        };
    }

    @Bean
    public SafClient safClient() {
        return new SafClient() {
            @Override
            public byte[] hentVedtakPdf(String journalpostId, String dokumentInfoId) {
                return new byte[0];
            }

            @Override
            public List<Journalpost> hentJournalposter(Fnr fnr) {
                return emptyList();
            }

            @Override
            public JournalpostGraphqlResponse hentJournalpost(String journalpostId) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VeilarbveilederClient veilederOgEnhetClient() {
        return new VeilarbveilederClient() {
            @Override
            public String hentEnhetNavn(String enhetId) {
                return TEST_OPPFOLGINGSENHET_NAVN;
            }

            @Override
            public String hentVeilederNavn(String veilederIdent) {
                return "VEILEDER VEILEDERSEN";
            }

            @Override
            public Veileder hentVeileder(String veilederIdent) {
                return new Veileder(TEST_VEILEDER_IDENT, "VEILEDER VEILEDERSEN");
            }



            @Override
            public VeilederEnheterDTO hentInnloggetVeilederEnheter() {
                List<PortefoljeEnhet> enheter = List.of(
                        new PortefoljeEnhet(TEST_OPPFOLGINGSENHET_ID, TEST_OPPFOLGINGSENHET_NAVN)
                );
                return new VeilederEnheterDTO(TEST_VEILEDER_IDENT, enheter);
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public DokarkivClient dokarkivClient() {
        return new DokarkivClient() {
            @Override
            public OpprettetJournalpostDTO opprettJournalpost(OpprettJournalpostDTO opprettJournalpostDTO) {
                return new OpprettetJournalpostDTO(
                        TEST_JOURNALPOST_ID,
                        true,
                        List.of(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID)));
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public RegoppslagClient regoppslagClient() {
        return new RegoppslagClient() {
            @NotNull
            @Override
            public RegoppslagResponseDTO hentPostadresse(@NotNull RegoppslagRequestDTO dto) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return null;
            }
        };
    }

    @Bean
    public DokdistribusjonClient dokdistribusjonClient() {
        return new DokdistribusjonClient() {
            @Override
            public DistribuerJournalpostResponsDTO distribuerJournalpost(DistribuerJournalpostDTO dto) {
                return new DistribuerJournalpostResponsDTO(TEST_DOKUMENT_BESTILLING_ID);
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public DokdistkanalClient dokdistkanalClient() {
        return new DokdistkanalClient() {
            @Override
            public @NotNull BestemDistribusjonskanalResponseDTO bestemDistribusjonskanal(@NotNull Fnr brukerFnr) {
                return new BestemDistribusjonskanalResponseDTO(BestemDistribusjonskanalResponseDTO.Distribusjonskanal.PRINT.toString(), "BRUKER_SDP_MANGLER_VARSELINFO", "Bruker skal varsles, men finner hverken mobiltelefonnummer eller e-postadresse");
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }
}
