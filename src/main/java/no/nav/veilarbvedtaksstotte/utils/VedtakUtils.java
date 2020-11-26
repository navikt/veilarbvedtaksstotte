package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus;

import java.time.LocalDateTime;
import java.util.List;

public class VedtakUtils {

    public static int tellVedtakEtterDato(List<Vedtak> alleVedtak, LocalDateTime dato) {
        if (alleVedtak.isEmpty()) return 0;

        return (int) alleVedtak.stream()
                .filter(vedtak -> dato.isAfter(vedtak.getSistOppdatert()))
                .count();
    }

    public static boolean erBeslutterProsessStartet(BeslutterProsessStatus beslutterProsessStatus) {
        return beslutterProsessStatus != null;
    }

}
