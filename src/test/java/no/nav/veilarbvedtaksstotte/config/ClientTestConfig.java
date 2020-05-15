package no.nav.veilarbvedtaksstotte.config;

import no.nav.common.health.HealthCheckResult;
import no.nav.veilarbvedtaksstotte.client.api.*;
import no.nav.veilarbvedtaksstotte.domain.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ClientTestConfig {

    @Bean
    public ArenaClient arenaClient() {
        return new ArenaClient() {
            @Override
            public String oppfolgingsenhet(String fnr) {
                return null;
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
                return null;
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
                return null;
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
                return null;
            }

            @Override
            public OppfolgingDTO hentOppfolgingData(String fnr) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public PamCvClient pamCvClient() {
        return new PamCvClient() {
            @Override
            public String hentCV(String fnr) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public PersonClient personClient() {
        return new PersonClient() {
            @Override
            public PersonNavn hentPersonNavn(String fnr) {
                return null;
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
                return null;
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
                return null;
            }

            @Override
            public Veileder hentVeileder(String veilederIdent) {
                return null;
            }

            @Override
            public VeilederEnheterDTO hentInnloggetVeilederEnheter() {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

}
