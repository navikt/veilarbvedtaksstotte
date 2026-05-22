package no.nav.veilarbvedtaksstotte.utils;

import no.nav.common.types.identer.Fnr;

import java.util.Arrays;
import java.util.List;

public class TestData {

    public static final long VEDTAK_ID_THAT_DOES_NOT_EXIST = 1000;

    public static final long SOME_ID = 42;
    public static final long SAK_ID = 10345;
    public static final Fnr TEST_FNR = Fnr.of("12345678912");
    public static final String TEST_AKTOR_ID = "123";
    public static final String TEST_VEILEDER_IDENT = "Z123456";
    public static final String TEST_VEILEDER_IDENT_2 = "Z654321";
    public static final String TEST_IKKE_ANSVARLIG_VEILEDER_IDENT = "Z987654";
    public static final String TEST_VEILEDER_NAVN = "Veileder Veilederen";
    public static final String TEST_OPPFOLGINGSENHET_ID = "1234";
    public static final String TEST_OPPFOLGINGSSAK = "SAK_1234";
    public static final String TEST_NAVKONTOR = "1234";
    public static final String TEST_OPPDATERT_NAVKONTOR = "4321";
    public static final String TEST_OPPFOLGINGSENHET_NAVN = "Nav Testheim";
    public static final String TEST_DIALOG_MELDING = "Dette er en melding";

    public static final List<String> TEST_KILDER = Arrays.asList("Svarene dine fra da du registrerte deg", "CV-en/jobbønskene dine på nav.no", "Svarene dine om behov for veiledning");

    public static final String TEST_JOURNALPOST_ID = "6723718";
    public static final String TEST_DOKUMENT_ID = "89704912";
    public static final String TEST_DOKUMENT_BESTILLING_ID = "abc123-321cba";

    public static final String TEST_BEGRUNNELSE = "Dette er en begrunnelse";

    public static final String TEST_BESLUTTER_NAVN = "Beslutter Besluttersen";
    public static final String TEST_BESLUTTER_IDENT = "Z748932";
    public static final String TEST_BESLUTTER_IDENT_2 = "Z239847";

    public static final String TEST_APP_NAME  = "veilarbvedtaksstotte";
}
