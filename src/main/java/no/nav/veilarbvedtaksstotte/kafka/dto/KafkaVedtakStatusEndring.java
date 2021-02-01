package no.nav.veilarbvedtaksstotte.kafka.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class KafkaVedtakStatusEndring {

    long vedtakId;

    String aktorId;

    VedtakStatusEndring vedtakStatusEndring;

    String ansvarligVeilederForVedtak;

    LocalDateTime timestamp;

    @Data
    public static class VedtakSendt extends KafkaVedtakStatusEndring {
        public VedtakSendt() { vedtakStatusEndring = VedtakStatusEndring.VEDTAK_SENDT; }

        Innsatsgruppe innsatsgruppe;
        Hovedmal hovedmal;
    }

    @Data
    public static class BliBeslutter extends KafkaVedtakStatusEndring {
        public BliBeslutter() { vedtakStatusEndring = VedtakStatusEndring.BLI_BESLUTTER; }

        String beslutterIdent;
        String beslutterNavn;
    }

    @Data
    public static class OvertaForBeslutter extends KafkaVedtakStatusEndring {
        public OvertaForBeslutter() { vedtakStatusEndring = VedtakStatusEndring.OVERTA_FOR_BESLUTTER; }

        String beslutterIdent;
        String beslutterNavn;
    }

    @Data
    public static class OvertaForVeileder extends KafkaVedtakStatusEndring {
        public OvertaForVeileder() { vedtakStatusEndring = VedtakStatusEndring.OVERTA_FOR_VEILEDER; }

        String veilederIdent;
        String veilederNavn;
    }

}
