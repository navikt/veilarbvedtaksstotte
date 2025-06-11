package no.nav.veilarbvedtaksstotte.domain.vedtak;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class Vedtak {
    long id;
    String aktorId;
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    VedtakStatus vedtakStatus;
    LocalDateTime utkastSistOppdatert;
    LocalDateTime vedtakFattet;
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
    String dokumentbestillingId;
    Boolean kanDistribueres;
    BeslutterProsessStatus beslutterProsessStatus;
    UUID referanse;

    @JsonIgnore
    boolean sender;

    public long getId() {
        return this.id;
    }

    public String getAktorId() {
        return this.aktorId;
    }

    public Hovedmal getHovedmal() {
        return this.hovedmal;
    }

    public Innsatsgruppe getInnsatsgruppe() {
        return this.innsatsgruppe;
    }

    public VedtakStatus getVedtakStatus() {
        return this.vedtakStatus;
    }

    public LocalDateTime getUtkastSistOppdatert() {
        return this.utkastSistOppdatert;
    }

    public LocalDateTime getVedtakFattet() {
        return this.vedtakFattet;
    }

    public LocalDateTime getUtkastOpprettet() {
        return this.utkastOpprettet;
    }

    public String getBegrunnelse() {
        return this.begrunnelse;
    }

    public String getVeilederIdent() {
        return this.veilederIdent;
    }

    public String getVeilederNavn() {
        return this.veilederNavn;
    }

    public String getOppfolgingsenhetId() {
        return this.oppfolgingsenhetId;
    }

    public String getOppfolgingsenhetNavn() {
        return this.oppfolgingsenhetNavn;
    }

    public String getBeslutterIdent() {
        return this.beslutterIdent;
    }

    public String getBeslutterNavn() {
        return this.beslutterNavn;
    }

    public boolean isGjeldende() {
        return this.gjeldende;
    }

    public List<String> getOpplysninger() {
        return this.opplysninger;
    }

    public String getJournalpostId() {
        return this.journalpostId;
    }

    public String getDokumentInfoId() {
        return this.dokumentInfoId;
    }

    public String getDokumentbestillingId() {
        return this.dokumentbestillingId;
    }

    public Boolean getKanDistribueres() {
        return this.kanDistribueres;
    }

    public BeslutterProsessStatus getBeslutterProsessStatus() {
        return this.beslutterProsessStatus;
    }

    public boolean isSender() {
        return this.sender;
    }

    public UUID getReferanse() {
        return this.referanse;
    }

    public Vedtak settId(Long id) { //Kotlin ser ikke lombok settere, men "vanlig" set-metoder går på bekostning av lombok sine. Lager derfor egen setter-metoder på norsk
        this.id = id;
        return this;
    }

    public Vedtak settAktorId(String aktorId) {
        this.aktorId = aktorId;
        return this;
    }

    public Vedtak settHovedmal(Hovedmal hovedmal) {
        this.hovedmal = hovedmal;
        return this;
    }

    public Vedtak settInnsatsgruppe(Innsatsgruppe innsatsgruppe) {
        this.innsatsgruppe = innsatsgruppe;
        return this;
    }

    public Vedtak settOppfolgingsenhetId(String oppfolgingsenhetId) {
        this.oppfolgingsenhetId = oppfolgingsenhetId;
        return this;
    }

    public Vedtak settUtkastOpprettet(LocalDateTime utkastOpprettet) {
        this.utkastOpprettet = utkastOpprettet;
        return this;
    }

    public Vedtak settVedtakFattet(LocalDateTime vedtakFattet) {
        this.vedtakFattet = vedtakFattet;
        return this;
    }

    public Vedtak settUtkastSistOppdatert(LocalDateTime utkastSistOppdatert) {
        this.utkastSistOppdatert = utkastSistOppdatert;
        return this;
    }

    public Vedtak settVeilederIdent(String veilederIdent) {
        this.veilederIdent = veilederIdent;
        return this;
    }

    public Vedtak settGjeldende(boolean gjeldende) {
        this.gjeldende = gjeldende;
        return this;
    }

    public Vedtak settReferanse(UUID referanse) {
        this.referanse = referanse;
        return this;
    }
    public Vedtak settBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
        return this;
    }
}
