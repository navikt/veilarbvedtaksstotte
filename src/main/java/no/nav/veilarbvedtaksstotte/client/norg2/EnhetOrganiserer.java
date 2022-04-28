package no.nav.veilarbvedtaksstotte.client.norg2;

import lombok.Value;
import no.nav.common.types.identer.EnhetId;

@Value
public class EnhetOrganiserer {
    EnhetId nr;
    String navn;
}
