package no.nav.veilarbvedtaksstotte.mock;

import no.nav.common.aktorregisterklient.AktorregisterKlient;
import no.nav.common.aktorregisterklient.IdentOppslag;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_FNR;

public class AktorregisterKlientMock implements AktorregisterKlient {

    @Override
    public String hentFnr(String aktorId) {
        return TEST_FNR;
    }

    @Override
    public String hentAktorId(String fnr) {
        return TEST_AKTOR_ID;
    }

    @Override
    public List<IdentOppslag> hentFnr(List<String> list) {
        return list.stream()
                .map(aktorId -> new IdentOppslag(aktorId, aktorId + "fnr"))
                .collect(Collectors.toList());
    }

    @Override
    public List<IdentOppslag> hentAktorId(List<String> list) {
        return list.stream()
                .map(fnr -> new IdentOppslag(fnr, fnr + "aktorId"))
                .collect(Collectors.toList());
    }

}
