package no.nav.fo.veilarbvedtaksstotte.domain;

import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;

import java.time.LocalDateTime;

public class ArkivertVedtak {

    public String journalpostId;

    public String dokumentInfoId;

    public String journalforendeEnhet;

    public String journalfortAvNavn;

    public LocalDateTime datoOpprettet;

    public boolean erGjeldende;

    public Innsatsgruppe gjeldendeInnsatsgruppe;

}
