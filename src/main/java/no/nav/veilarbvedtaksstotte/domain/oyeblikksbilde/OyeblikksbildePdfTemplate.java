package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

public enum OyeblikksbildePdfTemplate {
    CV_OG_JOBBPROFIL("oyeblikkbilde-cv", "oyeblikksbilde-cv", "CV-en/jobbønskene dine på nav.no"),
    REGISTRERINGSINFO("oyeblikkbilde-registrering", "Svarene dine fra da du registrerte deg"),
    ARBEIDSSOKERREGISTRET("oyeblikkbilde-arbeidssokerregistret", "oyeblikksbilde-arbeidssokerregistret", "Det du fortalte oss da du ble registrert som arbeidssoker"),
    EGENVURDERING("oyeblikkbilde-behovsvurdering", "oyeblikksbilde-behovsvurdering", "Svarene dine om behov for veiledning"),
    EGENVURDERINGV2("oyeblikkbilde-egenvurderingV2", "Svarene dine om behov for veiledning");


    public final String templateName;
    public final String newTemplateName;
    public final String fileName;

    OyeblikksbildePdfTemplate(String templateName, String fileName) {
        this(templateName, templateName, fileName);
    }

    OyeblikksbildePdfTemplate(String templateName, String newTemplateName, String fileName) {
        this.templateName = templateName;
        this.newTemplateName = newTemplateName;
        this.fileName = fileName;
    }
}
