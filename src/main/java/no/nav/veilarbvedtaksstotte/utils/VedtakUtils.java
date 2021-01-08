package no.nav.veilarbvedtaksstotte.utils;

import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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

    public static boolean erKilderLike(List<String> gamleKilder, List<String> nyeKilder) {
        if (gamleKilder.size() != nyeKilder.size()) return false;

        for (int i = 0; i < gamleKilder.size(); i++) {
            if (!Objects.equals(gamleKilder.get(i), nyeKilder.get(i))) {
                return false;
            }
        }

        return true;
    }

}
