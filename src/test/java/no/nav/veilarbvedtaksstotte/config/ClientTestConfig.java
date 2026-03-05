package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.BrukerIdenter;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NorskIdent;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.ArbeidssoekerregisteretApiOppslagV2Client;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.EgenvurderingDialogResponse;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.EgenvurderingDialogTjenesteClient;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.AggregertPeriode;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Annet;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.AvviksType;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Beskrivelse;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.BeskrivelseMedDetaljer;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Bruker;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.BrukerType;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Helse;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.JaNeiVetIkke;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Jobbsituasjon;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Metadata;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.OpplysningerOmArbeidssoeker;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.PeriodeStartet;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Profilering;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.ProfilertTil;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.TidspunktFraKilde;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model.Utdanning;
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
import no.nav.veilarbvedtaksstotte.client.pdf.BrevdataDto;
import no.nav.veilarbvedtaksstotte.client.pdf.CvInnholdMedMottakerDto;
import no.nav.veilarbvedtaksstotte.client.pdf.EgenvurderingMedMottakerDto;
import no.nav.veilarbvedtaksstotte.client.pdf.OpplysningerOmArbeidssoekerMedProfileringMedMottakerDto;
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient;
import no.nav.veilarbvedtaksstotte.client.person.OpplysningerOmArbeidssoekerMedProfilering;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.dto.Adressebeskyttelse;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvErrorStatus;
import no.nav.veilarbvedtaksstotte.client.person.dto.FodselsdatoOgAr;
import no.nav.veilarbvedtaksstotte.client.person.dto.Gradering;
import no.nav.veilarbvedtaksstotte.client.person.dto.PersonNavn;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagRequestDTO;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingStatusDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.SakDTO;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.domain.Malform;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
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
    public ArbeidssoekerregisteretApiOppslagV2Client arbeidssoekerregisteretApiOppslagV2Client() {
        return new ArbeidssoekerregisteretApiOppslagV2Client() {
            @Override
            public AggregertPeriode hentEgenvurdering(@NotNull NorskIdent norskIdent) {
                return new AggregertPeriode(
                        UUID.fromString("f6197da3-5aed-4c65-b19b-cd43164a567b"),
                        "01990112345",
                        new PeriodeStartet(
                                PeriodeStartet.Type.PERIODE_STARTET_V1,
                                new Metadata(
                                        LocalDateTime.parse("2025-11-26T14:57:39.724Z"),
                                        new Bruker(
                                                BrukerType.VEILEDER,
                                                "Z999999",
                                                "azure:undefined"
                                        ),
                                        "europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssokerregisteret-api-inngang:25.11.26.397-1",
                                        "Er over 18 år, er bosatt i Norge i henhold Folkeregisterloven",
                                        null
                                ),
                                LocalDateTime.parse("2025-11-26T14:57:39.724Z")
                        ),
                        null,
                        new OpplysningerOmArbeidssoeker(
                                OpplysningerOmArbeidssoeker.Type.OPPLYSNINGER_V4,
                                UUID.fromString("4a60081a-755c-4dd1-8094-d0db7a25d925"),
                                new Metadata(
                                        LocalDateTime.parse("2025-11-26T14:57:39.649Z"),
                                        new Bruker(
                                                BrukerType.VEILEDER,
                                                "Z999999",
                                                "azure:undefined"
                                        ),
                                        "europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssokerregisteret-api-inngang:25.11.26.397-1",
                                        "opplysning om arbeidssøker sendt inn",
                                        null
                                ),
                                new Utdanning(
                                        "4",
                                        JaNeiVetIkke.JA,
                                        JaNeiVetIkke.JA
                                ),
                                new Helse(
                                        JaNeiVetIkke.NEI
                                ),
                                new Jobbsituasjon(
                                        List.of(
                                                new BeskrivelseMedDetaljer(
                                                        Beskrivelse.HAR_SAGT_OPP,
                                                        Map.ofEntries(
                                                                Map.entry("stilling", "Annen stilling"),
                                                                Map.entry("stilling_styrk08", "00")
                                                        )
                                                )
                                        )
                                ),
                                new Annet(
                                        JaNeiVetIkke.NEI
                                ),
                                LocalDateTime.parse("2025-11-26T14:57:39.649Z")
                        ),
                        new Profilering(
                                Profilering.Type.PROFILERING_V1,
                                UUID.fromString("49dd4bd8-cef3-4ecc-a7f5-56dab0e8c128"),
                                UUID.fromString("4a60081a-755c-4dd1-8094-d0db7a25d925"),
                                new Metadata(
                                        LocalDateTime.parse("2025-11-26T14:57:40.49Z"),
                                        new Bruker(
                                                BrukerType.SYSTEM,
                                                "europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssokerregisteret-profilering:25.11.17.253-1",
                                                null
                                        ),
                                        "europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssokerregisteret-profilering:25.11.17.253-1",
                                        "opplysninger-mottatt",
                                        new TidspunktFraKilde(
                                                LocalDateTime.parse("2025-11-26T14:57:39.649Z"),
                                                AvviksType.FORSINKELSE
                                        )
                                ),
                                ProfilertTil.OPPGITT_HINDRINGER,
                                false,
                                48,
                                LocalDateTime.parse("2025-11-26T14:57:40.49Z")
                        ),
                        null,
                        null
                );
            }
        };
    }

    @Bean
    public EgenvurderingDialogTjenesteClient egenvurderingDialogTjenesteClient() {
        return new EgenvurderingDialogTjenesteClient() {
            @Override
            public EgenvurderingDialogResponse hentDialogId(@NotNull UUID arbeidssokerperiodeId) {
                return new EgenvurderingDialogResponse(
                        123456L
                );
            }
        };
    }

    @Bean
    public VeilarboppfolgingClient oppfolgingClient() {
        return new VeilarboppfolgingClient() {
            @Override
            public Optional<OppfolgingStatusDTO> erUnderOppfolging(Fnr fnr) {
                OppfolgingStatusDTO status = new OppfolgingStatusDTO();
                status.setErUnderOppfolging(true);
                return Optional.of(status);
            }

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
            public FodselsdatoOgAr hentFodselsdato(@NotNull Fnr fnr) {
                return new FodselsdatoOgAr(LocalDate.of(1990, 1, 1), 1990);
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
