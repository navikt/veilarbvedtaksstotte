package no.nav.fo.veilarbvedtaksstotte.mock;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

public class PepClientMock extends PepClient {

    public PepClientMock() {
      super(null, null, null);
    }

    @Override
    public void sjekkTilgangTilEnhet(String enhet) throws IngenTilgang, PepException {}

    @Override
    public boolean harTilgangTilEnhet(String enhet) throws PepException {
        return true;
    }
}
