package no.nav.veilarbvedtaksstotte.mock;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.IdentOppslag;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

import java.util.Collections;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;

public class AktorregisterClientMock implements AktorregisterClient {

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckResult.healthy();
    }

    @Override
    public Fnr hentFnr(AktorId aktorId) {
        return Fnr.of(TEST_FNR);
    }

    @Override
    public AktorId hentAktorId(Fnr fnr) {
        return AktorId.of(TEST_AKTOR_ID);
    }

    @Override
    public List<IdentOppslag> hentFnr(List<AktorId> list) {
        return Collections.emptyList();
    }

    @Override
    public List<IdentOppslag> hentAktorId(List<Fnr> list) {
        return Collections.emptyList();
    }

    @Override
    public List<AktorId> hentAktorIder(Fnr fnr) {
        return Collections.emptyList();
    }

}
