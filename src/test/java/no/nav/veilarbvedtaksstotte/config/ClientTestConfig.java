package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.api.*;
import no.nav.veilarbvedtaksstotte.client.api.dokarkiv.Journalpost;
import no.nav.veilarbvedtaksstotte.client.api.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.api.dokument.DokumentClient;
import no.nav.veilarbvedtaksstotte.client.api.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.client.api.dokument.SendDokumentDTO;
import no.nav.veilarbvedtaksstotte.client.api.oppfolging.OppfolgingClient;
import no.nav.veilarbvedtaksstotte.client.api.oppfolging.OppfolgingDTO;
import no.nav.veilarbvedtaksstotte.client.api.person.PersonNavn;
import no.nav.veilarbvedtaksstotte.client.api.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.api.registrering.RegistreringClient;
import no.nav.veilarbvedtaksstotte.client.api.registrering.RegistreringData;
import no.nav.veilarbvedtaksstotte.client.api.veilederogenhet.PortefoljeEnhet;
import no.nav.veilarbvedtaksstotte.client.api.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.client.api.veilederogenhet.VeilederEnheterDTO;
import no.nav.veilarbvedtaksstotte.client.api.veilederogenhet.VeiledereOgEnhetClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;

@Configuration
public class ClientTestConfig {

    @Bean
    public AktorOppslagClient aktorOppslagClient() {
        return new AktorOppslagClient() {
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
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public ArenaClient arenaClient() {
        return new ArenaClient() {
            @Override
            public String oppfolgingsenhet(String fnr) {
                return TEST_OPPFOLGINGSENHET_ID;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public DokumentClient dokumentClient() {
        return new DokumentClient() {
            @Override
            public DokumentSendtDTO sendDokument(SendDokumentDTO sendDokumentDTO) {
                return new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);
            }

            @Override
            public byte[] produserDokumentUtkast(SendDokumentDTO sendDokumentDTO) {
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
    public OppfolgingClient oppfolgingClient() {
        return new OppfolgingClient() {
            @Override
            public String hentServicegruppe(String fnr) {
                return "VURDU";
            }

            @Override
            public OppfolgingDTO hentOppfolgingData(String fnr) {
                OppfolgingDTO oppfolgingDTO = new OppfolgingDTO();
                oppfolgingDTO.setServicegruppe("VURDU");
                oppfolgingDTO.setOppfolgingsPerioder(Collections.emptyList());
                return oppfolgingDTO;
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
                PersonNavn personNavn = new PersonNavn();
                personNavn.setFornavn("TEST");
                personNavn.setMellomnavn(null);
                personNavn.setEtternavn("TESTERSEN");
                personNavn.setSammensattNavn("TEST TESTERSEN");
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
    public RegistreringClient registreringClient() {
        return new RegistreringClient() {
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
    public VeiledereOgEnhetClient veilederOgEnhetClient() {
        return new VeiledereOgEnhetClient() {
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

}
