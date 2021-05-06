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
public class UtkastetVedtak extends Vedtak {
    LocalDateTime sistOppdatert;
    LocalDateTime utkastOpprettet;
    BeslutterProsessStatus beslutterProsessStatus;
    VedtakStatus vedtakStatus = VedtakStatus.UTKAST;
}
