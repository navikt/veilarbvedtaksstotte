package no.nav.veilarbvedtaksstotte.mock;

import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;

import java.util.Map;

public class MetricsClientMock implements MetricsClient {
    @Override
    public void report(Event event) {

    }

    @Override
    public void report(String s, Map<String, Object> map, Map<String, String> map1, long l) {

    }
}
