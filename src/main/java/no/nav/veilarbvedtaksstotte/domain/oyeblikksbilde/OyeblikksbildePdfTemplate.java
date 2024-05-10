package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

public enum OyeblikksbildePdfTemplate {
    CV_OG_JOBBPROFIL("oyeblikkbilde-cv", "CV-en/jobbønskene dine på nav.no"),
    REGISTRERINGSINFO("oyeblikkbilde-registrering", "Svarene dine fra da du registrerte deg"),
    EGENVURDERING("oyeblikkbilde-behovsvurdering", "Svarene dine om behov for veiledning");


    public final String templateName;
    public final String fileName;

    OyeblikksbildePdfTemplate(String templateName, String fileName) {
        this.templateName = templateName;
        this.fileName = fileName;
    }
}
