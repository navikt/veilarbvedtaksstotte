package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.EnhetId;
import no.nav.veilarbvedtaksstotte.repository.TilgangskontrollRepository;
import no.nav.veilarbvedtaksstotte.repository.domain.EnhetTilgang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TilgangskontrollService {

    private final TilgangskontrollRepository tilgangskontrollRepository;

    @Autowired
    public TilgangskontrollService(TilgangskontrollRepository tilgangskontrollRepository) {
        this.tilgangskontrollRepository = tilgangskontrollRepository;
    }

    public List<EnhetTilgang> hentAlleTilganger() {
        return tilgangskontrollRepository.hentAlleTilganger();
    }

    public void lagNyTilgang(EnhetId enhetId) {
        tilgangskontrollRepository.leggTilTilgang(enhetId);
    }

    public void fjernTilgang(EnhetId enhetId) {
        tilgangskontrollRepository.fjernTilgang(enhetId);
    }

    public boolean harEnhetTilgang(EnhetId enhetId) {
        return tilgangskontrollRepository.harEnhetTilgang(enhetId);
    }

}
