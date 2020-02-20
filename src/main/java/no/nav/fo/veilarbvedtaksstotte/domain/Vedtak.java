package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.VedtakStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class Vedtak {
    long id;
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    VedtakStatus vedtakStatus;
    LocalDateTime sistOppdatert;
    LocalDateTime utkastOpprettet;
    String begrunnelse;
    String veilederIdent;
    String veilederNavn;
    String oppfolgingsenhetId;
    String oppfolgingsenhetNavn;
    String aktorId;
    String beslutterNavn;
    boolean gjeldende;
    boolean sendtTilBeslutter;
    List<String> opplysninger;
    String journalpostId;
    String dokumentInfoId;
}
