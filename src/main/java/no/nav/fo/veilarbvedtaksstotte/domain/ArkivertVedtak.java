package no.nav.fo.veilarbvedtaksstotte.domain;

import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;

import java.time.LocalDateTime;

public class ArkivertVedtak {

    public String journalpostId;

    public String dokumentInfoId;

    public String veilederNavn;

    public String oppfolgingsenhetId;

    public String oppfolgingsenhetNavn;

    public LocalDateTime datoOpprettet;

    public boolean erGjeldende;

    public Innsatsgruppe innsatsgruppe; // Vedtak fra arena har kun innsatsgruppe hvis erGjeldende == true

}
