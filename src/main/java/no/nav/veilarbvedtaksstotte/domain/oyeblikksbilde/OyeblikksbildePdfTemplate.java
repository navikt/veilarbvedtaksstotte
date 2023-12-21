package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

public enum OyeblikksbildePdfTemplate {
    CV_OG_JOBBPROFIL("oyeblikkbilde-cv"),
    REGISTRERINGSINFO("oyeblikkbilde-registrering"),
    EGENVURDERING("oyeblikkbilde-behovsvurdering");


    public final String templateName;

    OyeblikksbildePdfTemplate(String templateName) {
        this.templateName = templateName;
    }
}
