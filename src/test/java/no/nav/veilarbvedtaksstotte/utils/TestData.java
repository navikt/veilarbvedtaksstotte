package no.nav.veilarbvedtaksstotte.utils;

import no.nav.common.types.identer.Fnr;

import java.util.Arrays;
import java.util.List;

public class TestData {

    public final static long VEDTAK_ID_THAT_DOES_NOT_EXIST = 1000;

    public final static long SOME_ID = 42;
    public final static long SAK_ID = 10345;
    public final static Fnr TEST_FNR = Fnr.of("12345678912");
    public final static String TEST_AKTOR_ID = "123";
    public final static String TEST_VEILEDER_IDENT = "Z123456";
    public final static String TEST_IKKE_ANSVARLIG_VEILEDER_IDENT = "Z987654";
    public final static String TEST_VEILEDER_NAVN = "Veileder Veilederen";
    public final static String TEST_OPPFOLGINGSENHET_ID = "1234";
    public final static String TEST_OPPFOLGINGSSAK = "SAK_1234";
    public final static String TEST_OPPFOLGINGSENHET_NAVN = "NAV Testheim";
    public final static String TEST_DIALOG_MELDING = "Dette er en melding";

    public final static List<String> TEST_KILDER = Arrays.asList("Svarene dine fra da du registrerte deg", "CV-en/jobbønskene dine på nav.no", "Svarene dine om behov for veiledning");

    public final static String TEST_JOURNALPOST_ID = "6723718";
    public final static String TEST_DOKUMENT_ID = "89704912";
    public final static String TEST_DOKUMENT_BESTILLING_ID = "abc123-321cba";

    public final static String TEST_BEGRUNNELSE = "Dette er en begrunnelse";

    public final static String TEST_BESLUTTER_NAVN = "Beslutter Besluttersen";
    public final static String TEST_BESLUTTER_IDENT = "Z748932";
}
