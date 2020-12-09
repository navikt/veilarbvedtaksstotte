package no.nav.veilarbvedtaksstotte.domain.vedtak;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class Vedtak {
    long id;
    String aktorId;
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
    String beslutterIdent;
    String beslutterNavn;
    boolean gjeldende;
    List<String> opplysninger;
    String journalpostId;
    String dokumentInfoId;
    Boolean journalpostFerdigstilt;
    String dokumentbestillingId;
    BeslutterProsessStatus beslutterProsessStatus;

    @JsonIgnore
    boolean sender;
}
