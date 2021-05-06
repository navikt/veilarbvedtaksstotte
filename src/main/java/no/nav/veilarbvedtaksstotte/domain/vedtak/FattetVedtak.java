package no.nav.veilarbvedtaksstotte.domain.vedtak;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class FattetVedtak extends Vedtak {
    LocalDateTime vedtakFattet;
    boolean gjeldende;
    String journalpostId;
    String dokumentInfoId;
    String dokumentbestillingId;
    VedtakStatus vedtakStatus = VedtakStatus.SENDT;

}
