package no.nav.veilarbvedtaksstotte.domain.beslutteroversikt;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BeslutteroversiktSokFilter {

    List<String> enheter;

    BeslutteroversiktStatus status;

    boolean visMineBrukere;

    String navnEllerFnr;

}
