package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.client.api.veilederogenhet.Veileder;

import java.util.List;

@Data
@Accessors(chain = true)
public class VeilederePaEnhetDTO {
	List<Veileder> veilederListe;
}
