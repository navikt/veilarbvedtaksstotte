package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.BrukerIdenter;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbArenaOppfolging;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.*;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostResponsDTO;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingForPersonDTO;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingResponseDTO;
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetKontaktinformasjon;
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetOrganisering;
import no.nav.veilarbvedtaksstotte.client.norg2.EnhetStedsadresse;
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client;
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient;
import no.nav.veilarbvedtaksstotte.client.person.PersonNavn;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.RegistreringData;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagRequestDTO;
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagResponseDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.domain.Målform;
import org.jetbrains.annotations.NotNull;
import org.joda.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;

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
            public Optional<String> oppfolgingssak(Fnr fnr) {
                return Optional.of(TEST_OPPFOLGINGSSAK);
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
            public byte[] genererPdf(@NotNull Brevdata brevdata) {
                return new byte[0];
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public EgenvurderingClient egenvurderingClient() {
        return new EgenvurderingClient() {
            @Override
            public EgenvurderingResponseDTO hentEgenvurdering(EgenvurderingForPersonDTO egenvurderingForPersonDTO) {
                Map<String,String> egenvurderingstekster = new HashMap<>();
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
            public String hentCVOgJobbprofil(String fnr) {
                return "{ \"data\": \"Bruker har ikke delt CV/jobbprofil med NAV\"}";
            }

            @NotNull
            @Override
            public Målform hentMålform(@NotNull Fnr fnr) {
                return Målform.NB;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VeilarbregistreringClient registreringClient() {
        return new VeilarbregistreringClient() {
            @Override
            public String hentRegistreringDataJson(String fnr) {
                return null;
            }

            @Override
            public RegistreringData hentRegistreringData(String fnr) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
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
                        Arrays.asList(new OpprettetJournalpostDTO.DokumentInfoId(TEST_DOKUMENT_ID)));
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
}
