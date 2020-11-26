package no.nav.veilarbvedtaksstotte.client.api.dokarkiv;

public class Journalpost {

    public String journalpostId;

    public String tittel;

    public JournalpostDokument[] dokumenter;

    public static class JournalpostDokument {
        public String datoFerdigstilt;
        public String dokumentInfoId;
    }

}
