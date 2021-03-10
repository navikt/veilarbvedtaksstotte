package no.nav.veilarbvedtaksstotte.service;

import lombok.Value;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.utils.graphql.*;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Service
public class BrukerIdentService {
    PdlClient pdlClient;
    AktorOppslagClient aktorOppslagClient;
    UnleashService unleashService;

    @Autowired
    public BrukerIdentService(PdlClient pdlClient,
                              AktorOppslagClient aktorOppslagClient,
                              UnleashService unleashService
    ) {
        this.pdlClient = pdlClient;
        this.aktorOppslagClient = aktorOppslagClient;
        this.unleashService = unleashService;
    }


    public BrukerIdenter hentIdenter(EksternBrukerId brukerId) {
        // Ønsker mulighet til å bruke aktorregister i testmiljø for verdikjedetest mot Arena
        if (EnvironmentUtils.isDevelopment().orElse(false) &&
                unleashService.isPdlIdentOppslagDisabled()) {
            return hentFraAktorregister(brukerId);
        } else {
            return hentFraPdl(brukerId);
        }
    }

    // Ufullstendig implementasjon mot Aktørregisteret som ikke henter historiske identer
    private BrukerIdenter hentFraAktorregister(EksternBrukerId brukerId) {
        switch (brukerId.type()) {
            case AKTOR_ID:
                return new BrukerIdenter(
                        aktorOppslagClient.hentFnr((AktorId) brukerId),
                        (AktorId) brukerId, emptyList(), emptyList());
            case FNR:
                return new BrukerIdenter(
                        (Fnr) brukerId,
                        aktorOppslagClient.hentAktorId((Fnr) brukerId),
                        emptyList(), emptyList());
            default:
                throw new IllegalStateException("Ukjent EksternBrukerId " + brukerId.type());
        }
    }

    @Value
    static class HentIdenterResponse extends GraphqlResponse<HentIdenterResponse.HentIdenterResponseData> {
        List<GraphqlError> errors;
        HentIdenterResponseData data;

        @Value
        static class HentIdenterResponseData {
            IdenterResponseData hentIdenter;

            @Value
            static class IdenterResponseData {
                List<IdentData> identer;

                @Value
                static class IdentData {
                    String ident;
                    String gruppe;
                    boolean historisk;
                }
            }
        }
    }

    @Value
    static class HentIdentVariables {
        String ident;
    }

    private final GraphqlRequestBuilder<HentIdentVariables> hentFnrRequestBuilder =
            new GraphqlRequestBuilder<>("pdl/hent-identer-med-historikk.graphql");

    private final static String IDENT_GRUPPE_AKTORID = "AKTORID";

    private final static String IDENT_GRUPPE_FOLKEREGISTERIDENT = "FOLKEREGISTERIDENT";

    private BrukerIdenter hentFraPdl(EksternBrukerId brukerId) {
        HentIdenterResponse response = pdlClient.request(
                hentFnrRequestBuilder.buildRequest(new HentIdentVariables(brukerId.get())),
                HentIdenterResponse.class
        );

        GraphqlUtils.throwIfErrorOrMissingData(response);

        List<HentIdenterResponse.HentIdenterResponseData.IdenterResponseData.IdentData> identer =
                response.getData().hentIdenter.identer;

        List<HentIdenterResponse.HentIdenterResponseData.IdenterResponseData.IdentData> folkeregisteridenter =
                identer.stream().filter(ident -> IDENT_GRUPPE_FOLKEREGISTERIDENT.equals(ident.gruppe)).collect(toList());

        List<HentIdenterResponse.HentIdenterResponseData.IdenterResponseData.IdentData> aktorider =
                identer.stream().filter(ident -> IDENT_GRUPPE_AKTORID.equals(ident.gruppe)).collect(toList());

        return new BrukerIdenter(
                folkeregisteridenter.stream().filter(ident -> !ident.historisk).findFirst().map(ident -> Fnr.of(ident.ident)).orElseThrow(),
                aktorider.stream().filter(ident -> !ident.historisk).findFirst().map(ident -> AktorId.of(ident.ident)).orElseThrow(),
                folkeregisteridenter.stream().filter(ident -> ident.historisk).map(ident -> Fnr.of(ident.ident)).collect(toList()),
                aktorider.stream().filter(ident -> ident.historisk).map(ident -> AktorId.of(ident.ident)).collect(toList())
        );
    }
}
