package no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde;

public enum OyeblikksbildePdfTemplate {
    CV_OG_JOBBPROFIL("oyeblikkbilde-cv", "CV-en/jobbønskene dine på nav.no"),
    REGISTRERINGSINFO("oyeblikkbilde-registrering", "Svarene dine fra da du registrerte deg"),
    ARBEIDSSOKERREGISTRET("oyeblikkbilde-arbeidssokerregistret", "Det du fortalte oss da du ble registrert som arbeidssoker"),
    EGENVURDERING("oyeblikkbilde-behovsvurdering", "Svarene dine om behov for veiledning"),
    EGENVURDERINGV2("oyeblikkbilde-egenvurderingV2", "Svarene dine om behov for veiledning");


    public final String templateName;
    public final String fileName;

    OyeblikksbildePdfTemplate(String templateName, String fileName) {
        this.templateName = templateName;
        this.fileName = fileName;
    }
}
