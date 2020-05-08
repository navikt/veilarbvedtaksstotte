import no.nav.common.utils.SslUtils;
import org.springframework.boot.SpringApplication;

public class VeilarbvedtaksstotteApp {

    public static void main(String... args) {
//        NaisUtils.Credentials serviceUser = getCredentials("service_user");

//        setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty("AKTOER_V2_ENDPOINTURL"));
//        setProperty(DOKUMENT_API_PROPERTY_NAME, lagClusterUrl(VEILARBDOKUMENT));
//        setProperty(VEILARBVEILEDER_API_PROPERTY_NAME, lagClusterUrl(VEILARBVEILEDER));
//        setProperty(REGISTRERING_API_PROPERTY_NAME, lagClusterUrl(VEILARBREGISTRERING));
//        setProperty(EGENVURDERING_API_PROPERTY_NAME, lagClusterUrl(VEILARBVEDTAKINFO));
//        setProperty(VEILARBARENA_API_PROPERTY_NAME, lagClusterUrl(VEILARBARENA));
//        setProperty(SAF_API_PROPERTY_NAME, lagClusterUrl(SAF,false));
//        setProperty(VEILARBOPPFOLGING_API_PROPERTY_NAME, lagClusterUrl(VEILARBOPPFOLGING));
//        setProperty(VEILARBPERSON_API_PROPERTY_NAME, lagClusterUrl(VEILARBPERSON));
//
//        // PAM CV finnes ikke i Q1, gå mot Q0 istedenfor
//        // Oppgave i Q1 ligger på default namespace
//        if (EnvironmentUtils.requireEnvironmentName().equalsIgnoreCase("q1")) {
//            String pamCVUrl = lagClusterUrl(PAM_CV_API).replace("q1", "q0");
//            setProperty(CV_API_PROPERTY_NAME, pamCVUrl);
//        } else {
//            setProperty(CV_API_PROPERTY_NAME, lagClusterUrl(PAM_CV_API));
//        }

        SslUtils.setupTruststore();
        SpringApplication.run(VeilarbvedtaksstotteApp.class, args);
    }

}
