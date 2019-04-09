
import no.nav.apiapp.ApiApp;
import no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig;

public class Main {

    public static void main(String... args) {
        ApiApp.startApiApp(ApplicationConfig.class, args);
    }

}
