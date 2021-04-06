package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.*;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostResponsDTO;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient;
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.client.dokument.ProduserDokumentV2DTO;
import no.nav.veilarbvedtaksstotte.client.dokument.SendDokumentDTO;
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClient;
import no.nav.veilarbvedtaksstotte.client.person.PersonNavn;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.RegistreringData;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingsstatusDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilederEnheterDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;

@Configuration
public class ClientTestConfig {

    @Bean
    public AktorregisterClient aktorregisterClient() {
        return new AktorregisterClient() {
            @Override
            public Fnr hentFnr(AktorId aktorId) {
                return Fnr.of(TEST_FNR);
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
            public List<AktorId> hentAktorIder(Fnr fnr) {
                return Collections.emptyList();
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public Norg2Client norg2Client() {
        return new Norg2Client() {
            @Override
            public List<Enhet> alleAktiveEnheter() {
                return Collections.emptyList();
            }

            @Override
            public Enhet hentEnhet(String s) {
                return null;
            }

            @Override
            public Enhet hentTilhorendeEnhet(String s) {
                return null;
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
            public EnhetId oppfolgingsenhet(Fnr fnr) {
                return EnhetId.of(TEST_OPPFOLGINGSENHET_ID);
            }

            @Override
            public String oppfolgingssak(Fnr fnr) {
                return TEST_OPPFOLGINGSSAK;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VeilarbdokumentClient dokumentClient() {
        return new VeilarbdokumentClient() {
            @Override
            public DokumentSendtDTO sendDokument(SendDokumentDTO sendDokumentDTO) {
                return new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);
            }

            @Override
            public byte[] produserDokumentUtkast(SendDokumentDTO sendDokumentDTO) {
                return new byte[0];
            }

            @Override
            public byte[] produserDokumentV2(ProduserDokumentV2DTO produserDokumentV2DTO) {
                return new byte[0];
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VeilarbvedtakinfoClient egenvurderingClient() {
        return new VeilarbvedtakinfoClient() {
            @Override
            public String hentEgenvurdering(String fnr) {
                return "{ \"testData\": \"Egenvurdering\"}";
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
            public OppfolgingsstatusDTO hentOppfolgingData(String fnr) {
                OppfolgingsstatusDTO oppfolgingsstatusDTO = new OppfolgingsstatusDTO();
                oppfolgingsstatusDTO.setServicegruppe("VURDU");
                return oppfolgingsstatusDTO;
            }

            @Override
            public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(String fnr) {
                return Collections.emptyList();
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
            public List<Journalpost> hentJournalposter(String fnr) {
                return Collections.emptyList();
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
                Veileder veileder = new Veileder();
                veileder.setIdent(TEST_VEILEDER_IDENT);
                veileder.setFornavn("VEILEDER");
                veileder.setEtternavn("VEILEDERSEN");
                return veileder;
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
    public DokdistribusjonClient dokdistribusjonClient() {
        return new DokdistribusjonClient() {
            @Override
            public DistribuerJournalpostResponsDTO distribuerJournalpost(DistribuerJournalpostDTO request) {
                return new DistribuerJournalpostResponsDTO(TEST_DOKUMENT_BESTILLING_ID);
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }
}
