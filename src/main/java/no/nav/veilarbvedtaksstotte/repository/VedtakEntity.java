package no.nav.veilarbvedtaksstotte.repository;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Accessors(chain = true)
public class VedtakEntity {
    long id;
    String aktorId;
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    String begrunnelse;
    String veilederIdent;
    String veilederNavn;
    String oppfolgingsenhetId;
    String oppfolgingsenhetNavn;
    String beslutterIdent;
    String beslutterNavn;
    List<String> opplysninger;
    LocalDateTime sistOppdatert;
    LocalDateTime utkastOpprettet;
    BeslutterProsessStatus beslutterProsessStatus;
    VedtakStatus vedtakStatus;
    LocalDateTime vedtakFattet;
    boolean gjeldende;
    String journalpostId;
    String dokumentInfoId;
    String dokumentbestillingId;
}
