package no.nav.veilarbvedtaksstotte.client.dokarkiv.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Journalpost {

    public String journalpostId;

    public String tittel;

    public JournalpostDokument[] dokumenter;

    public static class JournalpostDokument {
        public String datoFerdigstilt;
        public String dokumentInfoId;
        public String brevkode;
    }

}
