package no.nav.fo.veilarbvedtaksstotte.domain;

public class Journalpost {

    public String journalpostId;

    public String tittel;

    public String journalforendeEnhet;

    public String journalfortAvNavn;

    public String datoOpprettet;

    public JournalpostDokument[] dokumenter;


    public class JournalpostDokument {

        public String dokumentInfoId;

    }

}
