package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BeslutteroversiktSok {
    int fra;
    int antall;

    BeslutteroversiktSokFilter filter;

    OrderByField orderByField;
    OrderByDirection orderByDirection;

    public enum OrderByField {
        BRUKER_ETTERNAVN,
        BRUKER_OPPFOLGINGSENHET_NAVN,
        BRUKER_FNR,
        VEDTAK_STARTET,
        STATUS,
        STATUS_ENDRET,
        BESLUTTER_NAVN,
        VEILEDER_NAVN
    }

    public enum OrderByDirection {
        ASC, DESC
    }
}
