package no.nav.veilarbvedtaksstotte.mock;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.dialogarena.aktor.AktorServiceImpl;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class AktorServiceMock extends AktorServiceImpl implements AktorService {

    private final String MOCK_FNR = "00123456789";
    private final String MOCK_AKTOR_ID = "1284181123913";

    @Override
    public Optional<String> getFnr(String aktorId) {
        return ofNullable(MOCK_FNR);
    }

    @Override
    public Optional<String> getAktorId(String fnr) {
        return ofNullable(MOCK_AKTOR_ID);
    }

}
