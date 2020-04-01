package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutteroversiktStatus;

import java.util.List;

@Data
@Accessors(chain = true)
public class BeslutteroversiktSokFilter {

    List<String> enheter;

    BeslutteroversiktStatus status;

    BrukerFilter brukerFilter;

    public enum BrukerFilter {
        MINE_BRUKERE, MANGLER_BESLUTTER
    }

}
